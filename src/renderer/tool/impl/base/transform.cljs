(ns renderer.tool.impl.base.transform
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.db :refer [App]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.db :refer [Element]]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.handlers :as history.handlers]
   [renderer.ruler.db :refer [Orientation]]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.theme.db :as theme.db]
   [renderer.tool.db :refer [Handle]]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds :refer [BBox]]
   [renderer.utils.element :as utils.element]
   [renderer.utils.math :refer [Vec2]]
   [renderer.utils.svg :as utils.svg]))

(def ScaleHandle [:enum
                  :middle-right
                  :middle-left
                  :top-middle :bottom-middle
                  :top-right :top-left
                  :bottom-right
                  :bottom-left])

(derive :transform ::tool.hierarchy/tool)

(derive :zoom ::tool.hierarchy/tool)

(defonce bbox-element (reagent/atom nil))

(rf/reg-fx
 ::update
 (fn [value]
   (reset! bbox-element value)))

(defmethod tool.hierarchy/properties :transform
  []
  {:icon "pointer"})

(defmethod tool.hierarchy/help [:transform :idle]
  []
  [:<>
   [:div "Click to select an element or drag to select by area."]
   [:div "Hold " [:span.shortcut-key "⇧"] " to add or remove elements to selection."]])

(defmethod tool.hierarchy/help [:transform :select]
  []
  [:div "Hold " [:span.shortcut-key "Alt"] " to select intersecting elements."])

(defmethod tool.hierarchy/help [:transform :translate]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction, and "
   [:span.shortcut-key "Alt"] " to clone."])

(defmethod tool.hierarchy/help [:transform :clone]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction. or release "
   [:span.shortcut-key "Alt"] " to move."])

(defmethod tool.hierarchy/help [:transform :scale]
  []
  [:<>
   [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions,"]
   [:div [:span.shortcut-key "⇧"] " to scale in place,"]
   [:div [:span.shortcut-key "Alt"] " to also scale children."]])

(m/=> hovered? [:-> App Element boolean? boolean?])
(defn hovered?
  [el intersecting?]
  (let [selection-bbox (element.hierarchy/bbox @bbox-element)]
    (if-let [el-bbox (:bbox el)]
      (if intersecting?
        (utils.bounds/intersect? el-bbox selection-bbox)
        (utils.bounds/contained? el-bbox selection-bbox))
      false)))

(m/=> reduce-by-area [:-> App boolean? ifn? App])
(defn reduce-by-area
  [db intersecting? f]
  (->> (element.handlers/entities db)
       (vals)
       (filter :visible)
       (reduce (fn [db el]
                 (cond-> db
                   (hovered? el intersecting?)
                   (f (:id el)))) db)))

(defmethod tool.hierarchy/on-pointer-move :transform
  [db {:keys [element] :as e}]
  (cond-> db
    (not (:shift-key e))
    (element.handlers/clear-ignored)

    :always
    (-> (element.handlers/clear-hovered)
        (tool.handlers/set-cursor (if (and element
                                           (or (= (:type element) :handle)
                                               (not (utils.element/root? element))))
                                    "move"
                                    "default")))

    (:id element)
    (element.handlers/hover (:id element))))

(defmethod tool.hierarchy/on-key-down :transform
  [db e]
  (cond-> db
    (:shift-key e)
    (element.handlers/ignore :bbox)

    (= (:key e) "ArrowUp")
    (element.handlers/translate [0 -1])

    (= (:key e) "ArrowDown")
    (element.handlers/translate [0 1])

    (= (:key e) "ArrowLeft")
    (element.handlers/translate [-1 0])

    (= (:key e) "ArrowRight")
    (element.handlers/translate [1 0])))

(defmethod tool.hierarchy/on-key-up :transform
  [db e]
  (cond-> db
    (not (:shift-key e))
    (element.handlers/clear-ignored)

    (= (:key e) "ArrowUp")
    (history.handlers/finalize "Move selection up")

    (= (:key e) "ArrowDown")
    (history.handlers/finalize "Move selection down")

    (= (:key e) "ArrowLeft")
    (history.handlers/finalize "Move selection left")

    (= (:key e) "ArrowRight")
    (history.handlers/finalize "Move selection right")))

(defmethod tool.hierarchy/on-pointer-down :transform
  [db {:keys [button element] :as e}]
  (cond-> db
    element
    (assoc :clicked-element element)

    (and (= button :right) (not= (:id element) :bbox))
    (element.handlers/toggle-selection (:id element) (:shift-key e))

    :always
    (element.handlers/ignore :bbox)))

(defmethod tool.hierarchy/on-pointer-up :transform
  [db {:keys [element] :as e}]
  (-> db
      (dissoc :clicked-element)
      (element.handlers/unignore :bbox)
      (element.handlers/toggle-selection (:id element) (:shift-key e))
      (history.handlers/finalize (if (:selected element)
                                   "Deselect element"
                                   "Select element"))))

(defmethod tool.hierarchy/on-double-click :transform
  [db e]
  (let [{{:keys [tag id]} :element} e]
    (if (= tag :g)
      (-> db
          (element.handlers/ignore id)
          (element.handlers/deselect id))
      (cond-> db
        (not= :canvas tag)
        (tool.handlers/activate :edit)))))

(defmethod tool.hierarchy/on-deactivate :transform
  [db]
  (-> (element.handlers/clear-ignored db)
      (element.handlers/clear-hovered)
      (dissoc :pivot-point)))

(defn select-rect
  [db intersecting?]
  (cond-> (utils.svg/select-box db)
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

(m/=> delta->offset-with-pivot-point [:-> ScaleHandle Vec2 BBox [:tuple Vec2 Vec2]])
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
  [handle offset bbox]
  (let [[x y] offset
        [min-x min-y max-x max-y] bbox
        [cx cy] (utils.bounds/center bbox)]
    (case handle
      :middle-right [[x 0] [min-x cy]]
      :middle-left [[(- x) 0] [max-x cy]]
      :top-middle [[0 (- y)] [cx max-y]]
      :bottom-middle [[0 y] [cx min-y]]
      :top-right [[x (- y)] [min-x max-y]]
      :top-left [[(- x) (- y)] [max-x max-y]]
      :bottom-right [[x y] [min-x min-y]]
      :bottom-left [[(- x) y] [max-x min-y]])))

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
        bbox (element.handlers/bbox db)
        [offset pivot-point] (delta->offset-with-pivot-point handle offset bbox)
        pivot-point (if in-place (utils.bounds/center bbox) pivot-point)
        offset (cond-> offset in-place (matrix/mul 2))
        dimensions (utils.bounds/->dimensions bbox)
        ratio (matrix/div (matrix/add dimensions offset) dimensions)
        ratio (cond-> ratio ratio-locked (lock-ratio handle))
        ;; TODO: Handle negative ratio, and position on recursive scale.
        ratio (mapv #(max % 0.01) ratio)]
    (-> (assoc db :pivot-point pivot-point)
        (element.handlers/scale ratio pivot-point recursive))))

(m/=> selectable? [:-> [:or Element Handle nil?] boolean?])
(defn selectable?
  [clicked-element]
  (and clicked-element
       (not (:selected clicked-element))
       (not= (:type clicked-element) :handle)
       (not= (:tag clicked-element) :canvas)))

(m/=> select-element [:-> App boolean? App])
(defn select-element
  [db multiple]
  (cond-> db
    (selectable? (:clicked-element db))
    (element.handlers/toggle-selection (-> db :clicked-element :id) multiple)))

(m/=> translate [:-> App Vec2 [:maybe Orientation] App])
(defn translate
  [db offset axis]
  (let [offset (case axis
                 :vertical [(first offset) 0]
                 :horizontal [0 (second offset)]
                 offset)]
    (reduce (fn [db id]
              (let [container (element.handlers/parent-container db id)
                    hovered-svg (element.handlers/hovered-svg db)
                    start-point (fn [el] (into [] (take 2) (:bbox el)))]
                (cond-> (element.handlers/translate db id offset)
                  (and (seq (element.handlers/selected db))
                       (empty? (rest (element.handlers/selected db)))
                       (contains? #{:translate :clone} (:state db))
                       (not= (:id (element.handlers/parent db id)) (:id hovered-svg))
                       (not (utils.element/svg? (element.handlers/entity db id))))
                  (cond->
                   :always
                    (element.handlers/set-parent (:id hovered-svg))

                    ;; FIXME: Handle nested containers.
                    (:bbox container)
                    (element.handlers/translate id (start-point container))

                    (:bbox hovered-svg)
                    (element.handlers/translate id (matrix/mul (start-point hovered-svg)
                                                               -1))))))
            db
            (element.handlers/top-ancestor-ids db))))

(defn drag-start->state
  [clicked-element]
  (cond
    (= (:type clicked-element) :element)
    (if (= (:tag clicked-element) :canvas) :select :translate)

    (= (:type clicked-element) :handle)
    (if (= (:action clicked-element) :scale) :scale :translate)

    :else
    :idle))

(defmethod tool.hierarchy/on-drag-start :transform
  [db e]
  (let [clicked-element (:clicked-element db)
        state (drag-start->state clicked-element)]
    (cond-> (-> (tool.handlers/set-state db state)
                (element.handlers/clear-hovered))
      (selectable? clicked-element)
      (-> (element.handlers/toggle-selection (-> db :clicked-element :id) (:shift-key e))
          (snap.handlers/delete-from-tree #{(-> db :clicked-element :id)})))))

(defmethod tool.hierarchy/on-drag :transform
  [db e]
  (let [ratio-locked? (or (:ctrl-key e) (element.handlers/ratio-locked? db))
        db (element.handlers/clear-ignored db)
        delta (tool.handlers/pointer-delta db)
        axis (when (:ctrl-key e)
               (if (> (abs (first delta)) (abs (second delta)))
                 :vertical
                 :horizontal))]
    (case (:state db)
      :select
      (-> (element.handlers/clear-hovered db)
          (tool.handlers/add-fx [::update (select-rect db (:alt-key e))])
          (reduce-by-area (:alt-key e) element.handlers/hover))

      :translate
      (if (:alt-key e)
        (tool.handlers/set-state db :clone)
        (-> (history.handlers/reset-state db)
            (select-element (:shift-key e))
            (translate delta axis)
            (snap.handlers/snap-with translate axis)
            (tool.handlers/set-cursor "move")))

      :clone
      (if (:alt-key e)
        (-> (history.handlers/reset-state db)
            (select-element (:shift-key e))
            (element.handlers/duplicate)
            (translate delta axis)
            (snap.handlers/snap-with translate axis)
            (tool.handlers/set-cursor "copy"))
        (tool.handlers/set-state db :translate))

      :scale
      (let [options {:ratio-locked ratio-locked?
                     :in-place (:shift-key e)
                     :recursive (:alt-key e)}]
        (-> (history.handlers/reset-state db)
            (tool.handlers/set-cursor "default")
            (scale (matrix/add delta (snap.handlers/nearest-delta db)) options)))

      :idle db)))

(defmethod tool.hierarchy/on-drag-end :transform
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (:shift-key e)) element.handlers/deselect-all)
                    (reduce-by-area (:alt-key e) element.handlers/select)
                    (tool.handlers/add-fx [::update nil])
                    (history.handlers/finalize "Modify selection"))
        :translate (history.handlers/finalize db "Move selection")
        :scale (history.handlers/finalize db "Scale selection")
        :clone (history.handlers/finalize db "Clone selection")
        :idle db)
      (tool.handlers/set-state :idle)
      (element.handlers/clear-hovered)
      (dissoc :clicked-element :pivot-point)))

(defmethod tool.hierarchy/snapping-points :transform
  [db]
  (let [elements (vals (element.handlers/entities db))
        selected (filter :selected elements)
        options (-> db :snap :options)]
    (cond
      (= (:state db) :scale)
      (when-let [el (:clicked-element db)]
        [(with-meta
           (matrix/add [(:x el) (:y el)]
                       (tool.handlers/pointer-delta db))
           {:label "scale handle"})])

      (not= (:state db) :idle)
      (cond-> (element.handlers/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (utils.bounds/->snapping-points (element.handlers/bbox db) options))))))

(defmethod tool.hierarchy/snapping-elements :transform
  [db]
  (let [non-selected-ids (element.handlers/non-selected-ids db)
        els (element.handlers/entities db)
        non-selected (select-keys els (vec non-selected-ids))]
    (filter :visible (vals non-selected))))

(m/=> size-label [:-> BBox any?])
(defn size-label
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x _min-y max-x y2] bbox
        x (+ min-x (/ (- max-x min-x) 2))
        y (+ y2 (/ (+ (/ theme.db/handle-size 2) 15) zoom))
        [w h] (utils.bounds/->dimensions bbox)
        text (str (.toFixed w 3) " x " (.toFixed h 3))]
    [utils.svg/label text [x y]]))

(m/=> area-label [:-> number? BBox any?])
(defn area-label
  [area bbox]
  (when area
    (let [zoom @(rf/subscribe [::document.subs/zoom])
          [min-x min-y max-x] bbox
          x (+ min-x (/ (- max-x min-x) 2))
          y (+ min-y (/ (- -15 (/ theme.db/handle-size 2)) zoom))
          text (str (.toFixed area 3) " px²")]
      [utils.svg/label text [x y]])))

(defmethod tool.hierarchy/render :transform
  []
  (let [state @(rf/subscribe [::tool.subs/state])
        selected-elements @(rf/subscribe [::element.subs/selected])
        bbox @(rf/subscribe [::element.subs/bbox])
        elements-area @(rf/subscribe [::element.subs/area])
        pivot-point @(rf/subscribe [::tool.subs/pivot-point])
        hovered-ids @(rf/subscribe [::element.subs/hovered])]
    [:<>
     (for [el selected-elements]
       (when (:bbox el)
         ^{:key (str (:id el) "-bbox")}
         [utils.svg/bounding-box (:bbox el) false]))

     (for [el hovered-ids]
       (when (:bbox el)
         ^{:key (str (:id el) "-bbox")}
         [utils.svg/bounding-box (:bbox el) true]))

     (when (and (pos? elements-area) (= state :scale) (seq bbox))
       [area-label elements-area bbox])

     (when (seq bbox)
       [:<>
        [tool.views/wrapping-bbox bbox]
        [tool.views/bounding-corners bbox]
        (when (= state :scale) [size-label bbox])])

     (when pivot-point
       [utils.svg/times pivot-point])

     [element.hierarchy/render @bbox-element]]))
