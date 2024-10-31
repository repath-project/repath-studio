(ns renderer.tool.impl.base.transform
  (:require
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.handle.views :as handle.v]
   [renderer.history.handlers :as history.h]
   [renderer.ruler.db :refer [Orientation]]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias s]
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
      (history.h/finalize (if (:selected element) "Deselect element" "Select element"))))

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

(defmethod hierarchy/deactivate :transform
  [db]
  (-> (element.h/clear-ignored db)
      (dissoc :pivot-point)))

(defn select-rect
  [db intersecting?]
  (cond-> (overlay/select-box db)
    (not intersecting?)
    (assoc-in [:attrs :fill] "transparent")))

(m/=> lock-ratio [:-> Vec2D ScaleHandle Vec2D])
(defn lock-ratio
  [[x y] handle]
  (let [[x y] (condp contains? handle
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(m/=> delta->offset-with-pivot-point [:-> ScaleHandle Vec2D Bounds [:tuple Vec2D Vec2D]])
(defn delta->offset-with-pivot-point
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
        [offset pivot-point] (delta->offset-with-pivot-point handle offset bounds)
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

(defn selectable?
  [clicked-element]
  (and clicked-element
       (not (:selected clicked-element))
       (not= (:type clicked-element) :handle)))

(m/=> select-element [:-> App boolean? App])
(defn select-element
  [db multiple]
  (cond-> db
    (selectable? (:clicked-element db))
    (element.h/select (-> db :clicked-element :id) multiple)))

(m/=> translate [:-> App Vec2D [:maybe Orientation] App])
(defn translate
  [db offset axis]
  (let [offset (case axis
                 :vertical [(first offset) 0]
                 :horizontal [0 (second offset)]
                 offset)]
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
            (element.h/top-ancestor-ids db))))

(defn drag-start->state
  [clicked-element]
  (cond
    (= (:type clicked-element) :element)
    (if (= (:tag clicked-element) :canvas) :select :translate)

    (= (:type clicked-element) :handle)
    (if (= (:action clicked-element) :scale) :scale :translate)

    :else
    :idle))

(defmethod hierarchy/drag-start :transform
  [db e]
  (let [clicked-element (:clicked-element db)
        state (drag-start->state clicked-element)]
    (cond-> (-> (h/set-state db state)
                (element.h/clear-hovered))
      (selectable? clicked-element)
      (element.h/select (-> db :clicked-element :id) (pointer/shift? e)))))

(defmethod hierarchy/drag :transform
  [db e]
  (let [state (:state db)
        ctrl? (pointer/ctrl? e)
        alt-key? (pointer/alt? e)
        ratio-locked? (or (pointer/ctrl? e) (element.h/ratio-locked? db))
        db (element.h/clear-ignored db)
        delta (h/pointer-delta db)
        axis (when ctrl? (if (> (abs (first delta)) (abs (second delta))) :vertical :horizontal))]
    (case state
      :select
      (-> (element.h/clear-hovered db)
          (h/set-temp (select-rect db alt-key?))
          (reduce-by-area (pointer/alt? e) element.h/hover))

      :translate
      (if alt-key?
        (h/set-state db :clone)
        (-> (history.h/swap db)
            (select-element (pointer/shift? e))
            (translate delta axis)
            (snap.h/snap-with translate axis)
            (h/set-cursor "default")))

      :clone
      (if alt-key?
        (-> (history.h/swap db)
            (select-element (pointer/shift? e))
            (element.h/duplicate)
            (translate delta axis)
            (snap.h/snap-with translate axis)
            (h/set-cursor "copy"))
        (h/set-state db :translate))

      :scale
      (let [options {:ratio-locked ratio-locked?
                     :in-place (pointer/shift? e)
                     :recursive (pointer/alt? e)}]
        (-> (history.h/swap db)
            (h/set-cursor "default")
            (scale delta options)
            (snap.h/snap-with scale options)))

      :idle db)))

(defmethod hierarchy/drag-end :transform
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (pointer/shift? e)) element.h/deselect)
                    (reduce-by-area (pointer/alt? e) element.h/select)
                    (h/dissoc-temp)
                    (history.h/finalize "Modify selection"))
        :translate (history.h/finalize db "Move selection")
        :scale (history.h/finalize db "Scale selection")
        :clone (history.h/finalize db "Clone selection")
        :idle db)
      (h/set-state :idle)
      (element.h/clear-hovered)
      (snap.h/update-tree)
      (dissoc :clicked-element :pivot-point)))

(defmethod hierarchy/snapping-bases :transform
  [db]
  (let [elements (vals (element.h/entities db))
        selected (filter :selected elements)
        options (-> db :snap :options)]
    (cond
      (not= (:state db) :idle)
      (cond-> (element.h/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (bounds/->snapping-points (element.h/bounds db) options))))))

(defmethod hierarchy/snapping-points :transform
  [db]
  (let [non-selected-ids (set/difference (set (keys (element.h/entities db)))
                                         (element.h/selected-with-descendant-ids db))
        non-selected (select-keys (element.h/entities db) (vec non-selected-ids))
        non-selected-visible (filter :visible (vals non-selected))]
    (element.h/snapping-points db non-selected-visible)))

(defmethod hierarchy/render :transform
  []
  (let [state @(rf/subscribe [::s/state])
        selected-elements @(rf/subscribe [::element.s/selected])
        bounds @(rf/subscribe [::element.s/bounds])
        elements-area @(rf/subscribe [::element.s/area])
        pivot-point @(rf/subscribe [::s/pivot-point])
        hovered-ids @(rf/subscribe [::element.s/hovered])]
    [:<>
     (when (not= state :scale)
       (for [el selected-elements]
         (when (:bounds el)
           ^{:key (str (:id el) "-bounds")}
           [overlay/bounding-box (:bounds el) false])))

     (for [el hovered-ids]
       (when (:bounds el)
         ^{:key (str (:id el) "-bounds")}
         [overlay/bounding-box (:bounds el) true]))

     (when (and (pos? elements-area) (= state :scale) (seq bounds))
       [overlay/area-label elements-area bounds])

     (when (seq bounds)
       [:<>
        [handle.v/wrapping-bounding-box bounds]
        (if (= state :scale)
          [overlay/size-label bounds]
          (when (not= state :translate)
            [handle.v/bounding-corners bounds]))])

     (when pivot-point
       [overlay/times pivot-point])]))
