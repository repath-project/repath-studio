(ns renderer.tool.transform.select
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.app.handlers :as app.h]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.math :refer [Vec2D]]
   [renderer.utils.pointer :as pointer]))

(def ScaleHandle [:enum
                  :middle-right
                  :middle-left
                  :top-middle :bottom-middle
                  :top-right :top-left
                  :bottom-right
                  :bottom-left])

(derive :select ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :select
  []
  {:icon "pointer-alt"})

(defmethod tool.hierarchy/help [:select :default]
  []
  [:<>
   [:div "Click to select an element or click and drag to select by area."]
   [:div "Hold " [:span.shortcut-key "⇧"] " to add or remove elements to selection."]])

(defmethod tool.hierarchy/help [:select :select]
  []
  [:div "Hold " [:span.shortcut-key "Alt"] " while dragging to select intersecting elements."])

(defmethod tool.hierarchy/help [:select :move]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction, and "
   [:span.shortcut-key "Alt"] " to clone."])

(defmethod tool.hierarchy/help [:select :clone]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction. or release "
   [:span.shortcut-key "Alt"] " to move."])

(defmethod tool.hierarchy/help [:select :scale]
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
              (f (:id el)))) db (filter :visible? (vals (element.h/elements db)))))

(defmethod tool.hierarchy/pointer-move :select
  [db {:keys [element] :as e}]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)

    :always
    (-> (element.h/clear-hovered)
        (assoc :cursor (if (and element
                                (or (= (:type element) :handle)
                                    (not (element/root? element))))
                         "move"
                         "default")))

    (:id element)
    (element.h/hover (:id element))))

(defmethod tool.hierarchy/key-down :select
  [db e]
  (cond-> db
    (pointer/shift? e)
    (element.h/ignore :bounding-box)))

(defmethod tool.hierarchy/key-up :select
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)))

(defmethod tool.hierarchy/pointer-down :select
  [db {:keys [button element] :as e}]
  (cond-> db
    element
    (assoc :clicked-element element)

    (and (= button :right) (not= (:id element) :bounding-box))
    (element.h/select (:id element) (pointer/shift? e))

    :always
    (element.h/ignore :bounding-box)))

(defmethod tool.hierarchy/pointer-up :select
  [db {:keys [element] :as e}]
  (-> db
      (dissoc :clicked-element)
      (element.h/clear-ignored :bounding-box)
      (element.h/select (:id element) (pointer/shift? e))
      (app.h/explain (if (:selected? element) "Deselect element" "Select element"))))

(defmethod tool.hierarchy/double-click :select
  [db {:keys [element]}]
  (if (= (:tag element) :g)
    (-> db
        (element.h/ignore (:id element))
        (element.h/deselect (:id element)))
    (cond-> db
      (not= :canvas (:tag element))
      (app.h/set-tool :edit))))

(defmethod tool.hierarchy/activate :select
  [db]
  (-> db
      (app.h/set-state :default)
      (app.h/set-cursor "default")))

(defmethod tool.hierarchy/deactivate :select
  [db]
  (element.h/clear-ignored db))

(defn select-rect
  [{:keys [adjusted-pointer-offset
           adjusted-pointer-pos
           active-document] :as db} intersecting?]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (cond-> (overlay/select-box adjusted-pointer-pos adjusted-pointer-offset zoom)
      (not intersecting?) (assoc-in [:attrs :fill] "transparent"))))

(defmethod tool.hierarchy/drag-start :select
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

(mx/defn offset-scale
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
  [db, [x y] :- Vec2D, lock-ratio? :- boolean?, in-place? :- boolean?, recur? :- boolean?]
  (let [handle (-> db :clicked-element :id)
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
        pivot-point (if in-place? [cx cy] pivot-point)
        offset (cond-> offset in-place? (mat/mul 2))
        ratio (mat/div (mat/add dimensions offset) dimensions)
        lock-ratio? (or lock-ratio? (every? #(-> % :tag tool.hierarchy/properties :locked-ratio?) (element.h/selected db)))
        ratio (cond-> ratio lock-ratio? (lock-ratio handle))
        ;; TODO: Handle negative/inverted ratio.
        ratio (mapv #(max 0 %) ratio)]
    (-> db
        (assoc :pivot-point pivot-point)
        (element.h/scale ratio pivot-point recur?))))

(mx/defn select-element
  [db, multi? :- boolean?]
  (cond-> db
    (and (:clicked-element db)
         (not (-> db :clicked-element :selected?))
         (not= (-> db :clicked-element :id) :bounding-box))
    (-> (element.h/select (-> db :clicked-element :id) multi?))))

(defmethod tool.hierarchy/drag :select
  [{:keys [state adjusted-pointer-offset adjusted-pointer-pos] :as db} e]
  (let [offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        ctrl? (pointer/ctrl? e)
        offset (cond-> offset
                 (and ctrl? (not= state :scale))
                 (pointer/lock-direction))
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
            (element.h/translate offset)
            (snap.h/snap-with element.h/translate)
            (app.h/set-cursor "default")))

      :clone
      (if alt-key?
        (-> db
            (history.h/swap)
            (select-element (pointer/shift? e))
            (element.h/duplicate offset)
            (snap.h/snap-with element.h/translate)
            (app.h/set-cursor "copy"))
        (app.h/set-state db :move))

      :scale
      (cond-> db
        :always
        (-> (history.h/swap)
            (app.h/set-cursor "default")
            (offset-scale offset (pointer/ctrl? e) (pointer/shift? e) (pointer/alt? e)))

        (not (pointer/ctrl? e))
        (snap.h/snap-with offset-scale false (pointer/shift? e) (pointer/alt? e)))

      :default db)))

(defmethod tool.hierarchy/drag-end :select
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
