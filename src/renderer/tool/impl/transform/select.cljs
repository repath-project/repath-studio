(ns renderer.tool.impl.transform.select
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.app.handlers :as app.h]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.pointer :as pointer]))

(def ScaleHandle [:enum
                  :middle-right
                  :middle-left
                  :top-middle :bottom-middle
                  :top-right :top-left
                  :bottom-right
                  :bottom-left])

(derive :select ::hierarchy/tool)

(defmethod hierarchy/properties :select
  []
  {:icon "pointer"})

(defmethod hierarchy/help [:select :default]
  []
  [:<>
   [:div "Click to select an element or click and drag to select by area."]
   [:div "Hold " [:span.shortcut-key "⇧"] " to add or remove elements to selection."]])

(defmethod hierarchy/help [:select :select]
  []
  [:div "Hold " [:span.shortcut-key "Alt"] " while dragging to select intersecting elements."])

(defmethod hierarchy/help [:select :move]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction, and "
   [:span.shortcut-key "Alt"] " to clone."])

(defmethod hierarchy/help [:select :clone]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction. or release "
   [:span.shortcut-key "Alt"] " to move."])

(defmethod hierarchy/help [:select :scale]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions, "
   [:span.shortcut-key "⇧"] " to scale in place, " [:span.shortcut-key "Alt"] " to also scale children."])

(mx/defn hovered? :- boolean?
  [db, el :- Element, intersecting? :- boolean?]
  (let [{{:keys [x y width height]} :attrs} (element.h/get-temp db)
        selection-bounds [x y (+ x width) (+ y height)]]
    (if-let [el-bounds (:bounds el)]
      (if intersecting?
        (bounds/intersect? el-bounds selection-bounds)
        (bounds/contained? el-bounds selection-bounds))
      false)))

(mx/defn reduce-by-area
  [db, intersecting? :- boolean?, f :- fn?]
  (reduce (fn [db el]
            (cond-> db
              (hovered? db el intersecting?)
              (f (:id el)))) db (filter :visible (vals (element.h/elements db)))))

(defmethod hierarchy/pointer-move :select
  [db {:keys [element] :as e}]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)

    :always
    (-> (element.h/clear-hovered)
        (app.h/set-cursor (if (and element (or (= (:type element) :handle)
                                               (not (element/root? element))))
                            "move"
                            "default")))

    (:id element)
    (element.h/hover (:id element))))

(defmethod hierarchy/key-down :select
  [db e]
  (cond-> db
    (pointer/shift? e)
    (element.h/ignore :bounding-box)))

(defmethod hierarchy/key-up :select
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)))

(defmethod hierarchy/pointer-down :select
  [db {:keys [button element] :as e}]
  (cond-> db
    element
    (assoc :clicked-element element)

    (and (= button :right) (not= (:id element) :bounding-box))
    (element.h/select (:id element) (pointer/shift? e))

    :always
    (element.h/ignore :bounding-box)))

(defmethod hierarchy/pointer-up :select
  [db {:keys [element] :as e}]
  (-> db
      (dissoc :clicked-element)
      (element.h/clear-ignored :bounding-box)
      (element.h/select (:id element) (pointer/shift? e))
      (app.h/explain (if (:selected element) "Deselect element" "Select element"))))

(defmethod hierarchy/double-click :select
  [db e]
  (let [{{:keys [tag id]} :element} e]
    (if (= tag :g)
      (-> db
          (element.h/ignore id)
          (element.h/deselect id))
      (cond-> db
        (not= :canvas tag)
        (app.h/set-tool :edit)))))

(defmethod hierarchy/activate :select
  [db]
  (-> db
      (app.h/set-state :default)
      (app.h/set-cursor "default")))

(defmethod hierarchy/deactivate :select
  [db]
  (element.h/clear-ignored db))

(defn select-rect
  [db intersecting?]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])]
    (cond-> (overlay/select-box (:adjusted-pointer-pos db) (:adjusted-pointer-offset db) zoom)
      (not intersecting?)
      (assoc-in [:attrs :fill] "transparent"))))

(defmethod hierarchy/drag-start :select
  [{:keys [clicked-element] :as db} _e]
  (app.h/set-state db (case (:tag clicked-element)
                        :canvas :select
                        :scale :scale
                        :move)))

(mx/defn lock-ratio :- Vec2D
  [[x y] :- Vec2D, handle :- ScaleHandle]
  (let [[x y] (condp contains? handle
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(mx/defn scale
  "Converts the x/y pointer offset to a scale ratio and a pivot point,
   to decouple this from the scaling method of the elements.

   :pivot-point
   + ─────────□──┬-------□
   │             |       |
   │             | ─ x ─ |
   │             │       │
   □ ─────────── ■       □
   |      |        ↖     │
   |      y          ↖   │
   |      |            ↖ │
   □----------□--------- ■ :bottom-right (active handle)"
  [db, delta :- Vec2D, {:keys [ratio-locked in-place recursive]}]
  (let [[x y] delta
        handle (-> db :clicked-element :id)
        bounds (element.h/bounds db)
        dimensions (bounds/->dimensions bounds)
        [x1 y1 x2 y2] bounds
        [cx cy] (bounds/center bounds)
        [offset pivot-point] (case handle
                               :middle-right [[x 0] [x1 cy]]
                               :middle-left [[(- x) 0] [x2 cy]]
                               :top-middle [[0 (- y)] [cx y2]]
                               :bottom-middle [[0 y] [cx y1]]
                               :top-right [[x (- y)] [x1 y2]]
                               :top-left [[(- x) (- y)] [x2 y2]]
                               :bottom-right [[x y] [x1 y1]]
                               :bottom-left [[(- x) y] [x2 y1]])
        pivot-point (if in-place [cx cy] pivot-point)
        offset (cond-> offset in-place (mat/mul 2))
        ratio (mat/div (mat/add dimensions offset) dimensions)
        ratio-locked (or ratio-locked (every? element/ratio-locked? (element.h/selected db)))
        ratio (cond-> ratio ratio-locked (lock-ratio handle))]
    ;; TODO: Enhance inverted ratio.
    (cond-> db
      (not-any? zero? ratio)
      (-> (assoc :pivot-point pivot-point)
          (element.h/scale (mapv abs ratio) pivot-point recursive)))))

(mx/defn select-element
  [db, multiple :- boolean?]
  (cond-> db
    (and (:clicked-element db)
         (not (-> db :clicked-element :selected))
         (not= (-> db :clicked-element :id) :bounding-box))
    (-> (element.h/select (-> db :clicked-element :id) multiple))))

(defmethod hierarchy/drag :select
  [db e]
  (let [delta (mat/sub (:adjusted-pointer-pos db) (:adjusted-pointer-offset db))
        state (:state db)
        ctrl? (pointer/ctrl? e)
        delta (cond-> delta (and ctrl? (not= state :scale)) pointer/lock-direction)
        alt-key? (pointer/alt? e)
        db (element.h/clear-ignored db)]
    (case state
      :select
      (-> db
          (element.h/assoc-temp (select-rect db alt-key?))
          (element.h/clear-hovered)
          (reduce-by-area (pointer/alt? e) element.h/hover))

      :move
      (if alt-key?
        (app.h/set-state db :clone)
        (-> db
            (history.h/swap)
            (select-element (pointer/shift? e))
            (element.h/translate delta)
            (snap.h/snap-with element.h/translate)
            (app.h/set-cursor "default")))

      :clone
      (if alt-key?
        (-> db
            (history.h/swap)
            (select-element (pointer/shift? e))
            (element.h/duplicate delta)
            (snap.h/snap-with element.h/translate)
            (app.h/set-cursor "copy"))
        (app.h/set-state db :move))

      :scale
      (cond-> db
        :always
        (-> (history.h/swap)
            (app.h/set-cursor "default")
            (scale delta {:ratio-locked (pointer/ctrl? e)
                          :in-place (pointer/shift? e)
                          :recursive (pointer/alt? e)}))

        (not (pointer/ctrl? e))
        (snap.h/snap-with scale {:ratio-locked false
                                 :in-place (pointer/shift? e)
                                 :recursive (pointer/alt? e)}))

      :default db)))

(defmethod hierarchy/drag-end :select
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (pointer/shift? e)) element.h/deselect)
                    (reduce-by-area (pointer/alt? e) element.h/select)
                    (element.h/dissoc-temp)
                    (app.h/explain "Modify selection"))
        :move (app.h/explain db "Move selection")
        :scale (app.h/explain db "Scale selection")
        :clone (app.h/explain db "Clone selection")
        :default db)
      (app.h/set-state :default)
      (element.h/clear-hovered)
      (dissoc :clicked-element :pivot-point)))
