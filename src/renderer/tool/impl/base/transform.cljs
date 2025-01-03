(ns renderer.tool.impl.base.transform
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.h]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.history.handlers :as history.h]
   [renderer.ruler.db :refer [Orientation]]
   [renderer.snap.handlers :as snap.h]
   [renderer.theme.db :as theme.db]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.tool.subs :as-alias s]
   [renderer.tool.views :as tool.v]
   [renderer.utils.bounds :as bounds :refer [Bounds]]
   [renderer.utils.element :as element]
   [renderer.utils.math :refer [Vec2]]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.svg :as svg]))

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
   [:span.shortcut-key "⇧"] " to scale in place, "
   [:span.shortcut-key "Alt"] " to also scale children."])

(m/=> hovered? [:-> App Element boolean? boolean?])
(defn hovered?
  [db el intersecting?]
  (let [selection-bounds (element.hierarchy/bounds (h/temp db))]
    (if-let [el-bounds (:bounds el)]
      (if intersecting?
        (bounds/intersect? el-bounds selection-bounds)
        (bounds/contained? el-bounds selection-bounds))
      false)))

(m/=> reduce-by-area [:-> App boolean? ifn? App])
(defn reduce-by-area
  [db intersecting? f]
  (reduce (fn [db el]
            (cond-> db
              (hovered? db el intersecting?)
              (f (:id el)))) db (filter :visible (vals (element.h/entities db)))))

(defmethod hierarchy/on-pointer-move :transform
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

(defmethod hierarchy/on-key-down :transform
  [db e]
  (cond-> db
    (pointer/shift? e)
    (element.h/ignore :bounding-box)))

(defmethod hierarchy/on-key-up :transform
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)))

(defmethod hierarchy/on-pointer-down :transform
  [db {:keys [button element] :as e}]
  (cond-> db
    element
    (assoc :clicked-element element)

    (and (= button :right) (not= (:id element) :bounding-box))
    (element.h/toggle-selection (:id element) (pointer/shift? e))

    :always
    (element.h/ignore :bounding-box)))

(defmethod hierarchy/on-pointer-up :transform
  [db {:keys [element] :as e}]
  (-> db
      (dissoc :clicked-element)
      (element.h/unignore :bounding-box)
      (element.h/toggle-selection (:id element) (pointer/shift? e))
      (history.h/finalize (if (:selected element)
                            "Deselect element"
                            "Select element"))))

(defmethod hierarchy/on-double-click :transform
  [db e]
  (let [{{:keys [tag id]} :element} e]
    (if (= tag :g)
      (-> db
          (element.h/ignore id)
          (element.h/deselect id))
      (cond-> db
        (not= :canvas tag)
        (h/activate :edit)))))

(defmethod hierarchy/on-deactivate :transform
  [db]
  (-> (element.h/clear-ignored db)
      (element.h/clear-hovered)
      (dissoc :pivot-point)))

(defn select-rect
  [db intersecting?]
  (cond-> (svg/select-box db)
    (not intersecting?)
    (assoc-in [:attrs :fill] "transparent")))

(m/=> lock-ratio [:-> Vec2 ScaleHandle Vec2])
(defn lock-ratio
  [[x y] handle]
  (let [[x y] (condp contains? handle
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(m/=> delta->offset-with-pivot-point [:-> ScaleHandle Vec2 Bounds [:tuple Vec2 Vec2]])
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

(def ScaleOptions
  [:map
   [:ratio-locked boolean?]
   [:in-place boolean?]
   [:recursive boolean?]])

(m/=> scale [:-> App Vec2 ScaleOptions App])
(defn scale
  [db offset options]
  (let [{:keys [ratio-locked in-place recursive]} options
        handle (-> db :clicked-element :id)
        bounds (element.h/bounds db)
        [offset pivot-point] (delta->offset-with-pivot-point handle offset bounds)
        pivot-point (if in-place (bounds/center bounds) pivot-point)
        offset (cond-> offset in-place (mat/mul 2))
        dimensions (bounds/->dimensions bounds)
        ratio (mat/div (mat/add dimensions offset) dimensions)
        ratio (cond-> ratio ratio-locked (lock-ratio handle))
        ;; TODO: Handle negative ratio, and position on recursive scale.
        ratio (mapv #(max % 0.01) ratio)]
    (-> (assoc db :pivot-point pivot-point)
        (element.h/scale ratio pivot-point recursive))))

(m/=> selectable? [:-> [:or Element Handle nil?] boolean?])
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
    (element.h/toggle-selection (-> db :clicked-element :id) multiple)))

(m/=> translate [:-> App Vec2 [:maybe Orientation] App])
(defn translate
  [db offset axis]
  (let [offset (case axis
                 :vertical [(first offset) 0]
                 :horizontal [0 (second offset)]
                 offset)]
    (reduce (fn [db id]
              (let [container (element.h/parent-container db id)
                    hovered-svg (element.h/hovered-svg db)]
                (cond-> (element.h/translate db id offset)
                  (and (seq (element.h/selected db))
                       (empty? (rest (element.h/selected db)))
                       (contains? #{:translate :clone} (:state db))
                       (not= (:id (element.h/parent db id)) (:id hovered-svg))
                       (not (element/svg? (element.h/entity db id))))
                  (cond->
                   :always
                    (element.h/set-parent (:id hovered-svg))

                    ;; FIXME: Handle nested containers.
                    (:bounds container)
                    (element.h/translate id (vec (take 2 (:bounds container))))

                    (:bounds hovered-svg)
                    (element.h/translate id (mat/mul (take 2 (:bounds hovered-svg))
                                                     -1))))))
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

(defmethod hierarchy/on-drag-start :transform
  [db e]
  (let [clicked-element (:clicked-element db)
        state (drag-start->state clicked-element)]
    (cond-> (-> (h/set-state db state)
                (element.h/clear-hovered))
      (selectable? clicked-element)
      (-> (element.h/toggle-selection (-> db :clicked-element :id) (pointer/shift? e))
          (snap.h/delete-from-tree #{(-> db :clicked-element :id)})))))

(defmethod hierarchy/on-drag :transform
  [db e]
  (let [state (:state db)
        ctrl? (pointer/ctrl? e)
        alt-key? (pointer/alt? e)
        ratio-locked? (or (pointer/ctrl? e) (element.h/ratio-locked? db))
        db (element.h/clear-ignored db)
        delta (h/pointer-delta db)
        axis (when ctrl?
               (if (> (abs (first delta)) (abs (second delta)))
                 :vertical
                 :horizontal))]
    (case state
      :select
      (-> (element.h/clear-hovered db)
          (h/set-temp (select-rect db alt-key?))
          (reduce-by-area (pointer/alt? e) element.h/hover))

      :translate
      (if alt-key?
        (h/set-state db :clone)
        (-> (history.h/reset-state db)
            (select-element (pointer/shift? e))
            (translate delta axis)
            (snap.h/snap-with translate axis)
            (h/set-cursor "default")))

      :clone
      (if alt-key?
        (-> (history.h/reset-state db)
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
        (-> (history.h/reset-state db)
            (h/set-cursor "default")
            (scale (mat/add delta (snap.h/nearest-delta db)) options)))

      :idle db)))

(defmethod hierarchy/on-drag-end :transform
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (pointer/shift? e)) element.h/deselect-all)
                    (reduce-by-area (pointer/alt? e) element.h/select)
                    (h/dissoc-temp)
                    (history.h/finalize "Modify selection"))
        :translate (history.h/finalize db "Move selection")
        :scale (history.h/finalize db "Scale selection")
        :clone (history.h/finalize db "Clone selection")
        :idle db)
      (h/set-state :idle)
      (element.h/clear-hovered)
      (dissoc :clicked-element :pivot-point)))

(defmethod hierarchy/snapping-points :transform
  [db]
  (let [elements (vals (element.h/entities db))
        selected (filter :selected elements)
        options (-> db :snap :options)]
    (cond
      (= (:state db) :scale)
      (when-let [el (:clicked-element db)]
        [(with-meta
           (mat/add [(:x el) (:y el)]
                    (h/pointer-delta db))
           {:label "scale handle"})])

      (not= (:state db) :idle)
      (cond-> (element.h/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (bounds/->snapping-points (element.h/bounds db) options))))))

(defmethod hierarchy/snapping-elements :transform
  [db]
  (let [non-selected-ids (element.h/non-selected-ids db)
        non-selected (select-keys (element.h/entities db) (vec non-selected-ids))]
    (filter :visible (vals non-selected))))

(m/=> size-label [:-> Bounds any?])
(defn size-label
  [bounds]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        [x1 _ x2 y2] bounds
        x (+ x1 (/ (- x2 x1) 2))
        y (+ y2 (/ (+ (/ theme.db/handle-size 2) 15) zoom))
        [width height] (bounds/->dimensions bounds)
        text (str (.toFixed width 2) " x " (.toFixed height 2))]
    [svg/label text [x y]]))

(m/=> area-label [:-> number? Bounds any?])
(defn area-label
  [area bounds]
  (when area
    (let [zoom @(rf/subscribe [::document.s/zoom])
          [x1 y1 x2 _y2] bounds
          x (+ x1 (/ (- x2 x1) 2))
          y (+ y1 (/ (- -15 (/ theme.db/handle-size 2)) zoom))
          text (str (.toFixed area 2) " px²")]
      [svg/label text [x y]])))

(defmethod hierarchy/render :transform
  []
  (let [state @(rf/subscribe [::s/state])
        selected-elements @(rf/subscribe [::element.s/selected])
        bounds @(rf/subscribe [::element.s/bounds])
        elements-area @(rf/subscribe [::element.s/area])
        pivot-point @(rf/subscribe [::s/pivot-point])
        hovered-ids @(rf/subscribe [::element.s/hovered])]
    [:<>
     (for [el selected-elements]
       (when (:bounds el)
         ^{:key (str (:id el) "-bounds")}
         [svg/bounding-box (:bounds el) false]))

     (for [el hovered-ids]
       (when (:bounds el)
         ^{:key (str (:id el) "-bounds")}
         [svg/bounding-box (:bounds el) true]))

     (when (and (pos? elements-area) (= state :scale) (seq bounds))
       [area-label elements-area bounds])

     (when (seq bounds)
       [:<>
        [tool.v/wrapping-bounding-box bounds]
        (case state
          :scale [size-label bounds]
          :idle [tool.v/bounding-corners bounds]
          nil)])

     (when pivot-point
       [svg/times pivot-point])]))
