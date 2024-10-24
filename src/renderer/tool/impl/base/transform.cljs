(ns renderer.tool.impl.base.transform
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.bounds :as bounds :refer [Bounds]]
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

(derive :transform ::hierarchy/tool)

(defmethod hierarchy/properties :transform
  []
  {:icon "pointer"})

(defmethod hierarchy/help [:transform :idle]
  []
  [:<>
   [:div "Click to select an element or click and drag to select by area."]
   [:div "Hold " [:span.shortcut-key "⇧"] " to add or remove elements to selection."]])

(defmethod hierarchy/help [:transform :select]
  []
  [:div "Hold " [:span.shortcut-key "Alt"] " while dragging to select intersecting elements."])

(defmethod hierarchy/help [:transform :translate]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction, and "
   [:span.shortcut-key "Alt"] " to clone."])

(defmethod hierarchy/help [:transform :clone]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction. or release "
   [:span.shortcut-key "Alt"] " to move."])

(defmethod hierarchy/help [:transform :scale]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions, "
   [:span.shortcut-key "⇧"] " to scale in place, " [:span.shortcut-key "Alt"] " to also scale children."])

(m/=> hovered? [:-> App Element boolean? boolean?])
(defn hovered?
  [db el intersecting?]
  (let [selection-bounds (element.hierarchy/bounds (h/temp db))]
    (if-let [el-bounds (:bounds el)]
      (if intersecting?
        (bounds/intersect? el-bounds selection-bounds)
        (bounds/contained? el-bounds selection-bounds))
      false)))

(m/=> reduce-by-area [:-> App boolean? fn? App])
(defn reduce-by-area
  [db intersecting? f]
  (reduce (fn [db el]
            (cond-> db
              (hovered? db el intersecting?)
              (f (:id el)))) db (filter :visible (vals (element.h/entities db)))))

(defmethod hierarchy/pointer-move :transform
  [db {:keys [element] :as e}]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)

    :always
    (-> (element.h/clear-hovered)
        (h/set-cursor (if (and element (or (= (:type element) :handle)
                                           (not (element/root? element))))
                        "move"
                        "default")))

    (:id element)
    (element.h/hover (:id element))))

(defmethod hierarchy/key-down :transform
  [db e]
  (cond-> db
    (pointer/shift? e)
    (element.h/ignore :bounding-box)))

(defmethod hierarchy/key-up :transform
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)))

(defmethod hierarchy/pointer-down :transform
  [db {:keys [button element] :as e}]
  (cond-> db
    element
    (assoc :clicked-element element)

    (and (= button :right) (not= (:id element) :bounding-box))
    (element.h/select (:id element) (pointer/shift? e))

    :always
    (element.h/ignore :bounding-box)))

(defmethod hierarchy/pointer-up :transform
  [db {:keys [element] :as e}]
  (-> db
      (dissoc :clicked-element)
      (element.h/clear-ignored :bounding-box)
      (element.h/select (:id element) (pointer/shift? e))
      (h/explain (if (:selected element) "Deselect element" "Select element"))))

(defmethod hierarchy/double-click :transform
  [db e]
  (let [{{:keys [tag id]} :element} e]
    (if (= tag :g)
      (-> db
          (element.h/ignore id)
          (element.h/deselect id))
      (cond-> db
        (not= :canvas tag)
        (h/activate :edit)))))

(defmethod hierarchy/activate :transform
  [db]
  (-> db
      (h/set-state :idle)
      (h/set-cursor "default")))

(defmethod hierarchy/deactivate :transform
  [db]
  (element.h/clear-ignored db))

(defn select-rect
  [db intersecting?]
  (cond-> (overlay/select-box db)
    (not intersecting?)
    (assoc-in [:attrs :fill] "transparent")))

(defmethod hierarchy/drag-start :transform
  [db _e]
  (let [{:keys [clicked-element]} db]
    (h/set-state db (cond
                      (= (:type clicked-element) :element)
                      (if (= (:tag clicked-element) :canvas) :select :translate)

                      (= (:type clicked-element) :handle)
                      (if (= (:action clicked-element) :scale) :scale :translate)

                      :else
                      :idle))))

(m/=> lock-ratio [:-> Vec2D ScaleHandle Vec2D])
(defn lock-ratio
  [[x y] handle]
  (let [[x y] (condp contains? handle
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(m/=> delta->scale-with-pivot-point [:-> ScaleHandle Vec2D Bounds [:tuple Vec2D Vec2D]])
(defn delta->scale-with-pivot-point
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
  [handle offset bounds]
  (let [[x y] offset
        [x1 y1 x2 y2] bounds
        [cx cy] (bounds/center bounds)]
    (case handle
      :middle-right [[x 0] [x1 cy]]
      :middle-left [[(- x) 0] [x2 cy]]
      :top-middle [[0 (- y)] [cx y2]]
      :bottom-middle [[0 y] [cx y1]]
      :top-right [[x (- y)] [x1 y2]]
      :top-left [[(- x) (- y)] [x2 y2]]
      :bottom-right [[x y] [x1 y1]]
      :bottom-left [[(- x) y] [x2 y1]])))

(m/=> scale [:-> App Vec2D map? App])
(defn scale
  [db offset {:keys [ratio-locked in-place recursive]}]
  (let [handle (-> db :clicked-element :id)
        bounds (element.h/bounds db)
        [offset pivot-point] (delta->scale-with-pivot-point handle offset bounds)
        pivot-point (if in-place (bounds/center bounds) pivot-point)
        offset (cond-> offset in-place (mat/mul 2))
        dimensions (bounds/->dimensions bounds)
        ratio (mat/div (mat/add dimensions offset) dimensions)
        ratio (cond-> ratio ratio-locked (lock-ratio handle))
        ;; TODO: Handle negative ratio, and position on recursive scale.
        ratio (mapv #(max % 0.01) ratio)]
    (-> db
        (assoc :pivot-point pivot-point)
        (element.h/scale ratio pivot-point recursive))))

(m/=> select-element [:-> App boolean? App])
(defn select-element
  [db multiple]
  (cond-> db
    (and (:clicked-element db)
         (not (-> db :clicked-element :selected))
         (not= (-> db :clicked-element :id) :bounding-box))
    (-> (element.h/select (-> db :clicked-element :id) multiple))))

(m/=> translate [:-> App Vec2D App])
(defn translate
  [db offset]
  (reduce (fn [db id]
            (let [container (element.h/parent-container db id)
                  hovered-svg-k (:id (element.h/hovered-svg db))]
              (cond-> db
                :always
                (element.h/translate id offset)

                (and (seq (element.h/selected db))
                     (empty? (rest (element.h/selected db)))
                     (contains? #{:translate :clone} (:state db))
                     (not= (:id (element.h/parent db id)) hovered-svg-k)
                     (not (element/svg? (element.h/entity db id))))
                (-> (element.h/set-parent hovered-svg-k)
                       ;; FIXME: Handle nested containers.
                    (element.h/translate id (take 2 (:bounds container)))
                    (element.h/translate id (mat/mul (take 2 (:bounds (element.h/hovered-svg db))) -1))))))
          db
          (element.h/top-ancestor-ids db)))

(defmethod hierarchy/drag :transform
  [db e]
  (let [state (:state db)
        ctrl? (pointer/ctrl? e)
        alt-key? (pointer/alt? e)
        ratio-locked? (or (pointer/ctrl? e) (element.h/ratio-locked? db))
        db (element.h/clear-ignored db)
        delta (cond-> (h/pointer-delta db)
                (and ctrl? (not= state :scale))
                pointer/lock-direction)]
    (case state
      :select
      (-> db
          (h/set-temp (select-rect db alt-key?))
          (element.h/clear-hovered)
          (reduce-by-area (pointer/alt? e) element.h/hover))

      :translate
      (if alt-key?
        (h/set-state db :clone)
        (-> db
            (history.h/swap)
            (select-element (pointer/shift? e))
            (translate delta)
            (snap.h/snap-with translate)
            (h/set-cursor "default")))

      :clone
      (if alt-key?
        (-> db
            (history.h/swap)
            (element.h/duplicate)
            (translate delta)
            (snap.h/snap-with element.h/translate)
            (h/set-cursor "copy"))
        (h/set-state db :translate))

      :scale
      (cond-> db
        :always
        (-> (history.h/swap)
            (h/set-cursor "default")
            (scale delta {:ratio-locked ratio-locked?
                          :in-place (pointer/shift? e)
                          :recursive (pointer/alt? e)}))

        (not ratio-locked?)
        (snap.h/snap-with scale {:ratio-locked false
                                 :in-place (pointer/shift? e)
                                 :recursive (pointer/alt? e)}))

      :idle db)))

(defmethod hierarchy/drag-end :transform
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (pointer/shift? e)) element.h/deselect)
                    (reduce-by-area (pointer/alt? e) element.h/select)
                    (h/dissoc-temp)
                    (h/explain "Modify selection"))
        :translate (h/explain db "Move selection")
        :scale (h/explain db "Scale selection")
        :clone (h/explain db "Clone selection")
        :idle db)
      (h/set-state :idle)
      (element.h/clear-hovered)
      (dissoc :clicked-element :pivot-point)))

(defmethod hierarchy/snapping-bases :transform
  [db]
  (let [elements (vals (element.h/entities db))
        selected (filter :selected elements)
        selected-visible (filter :visible selected)
        options (-> db :snap :options)]
    (cond
      (and (contains? #{:translate :clone} (:state db)) (seq selected))
      (reduce (fn [points el] (into points (element/snapping-points el options)))
              (if (seq (rest selected))
                (bounds/->snapping-points (element.h/bounds db) options)
                [])
              selected-visible)

      (= (:state db) :scale)
      [(mat/add [(-> db :clicked-element :x) (-> db :clicked-element :y)]
                (mat/sub (:adjusted-pointer-pos db) (:adjusted-pointer-offset db)))])))
