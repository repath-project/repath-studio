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
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
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

(defonce select-box (reagent/atom nil))

(rf/reg-fx
 ::set-select-box
 (fn [value]
   (reset! select-box value)))

(defmethod tool.hierarchy/properties :transform
  []
  {:icon "pointer"
   :label (t [::label "Transform"])})

(defmethod tool.hierarchy/help [:transform :idle]
  []
  [:<>
   (t [::idle-click [:div "Click to select an element or drag to select by
                           area."]])
   (t [::idle-hold [:div "Hold %1 to add or remove elements to selection."]]
      [[:span.shortcut-key "⇧"]])])

(defmethod tool.hierarchy/help [:transform :select]
  []
  (t [::select [:div "Hold %1 to select intersecting elements."]]
     [[:span.shortcut-key "Alt"]]))

(defmethod tool.hierarchy/help [:transform :translate]
  []
  (t [::translate [:div "Hold %1 to restrict direction, and %2 to clone."]]
     [[:span.shortcut-key "Ctrl"]
      [:span.shortcut-key "Alt"]]))

(defmethod tool.hierarchy/help [:transform :clone]
  []
  (t [::clone [:div "Hold %1 to restrict direction. or release %2 to move"]]
     [[:span.shortcut-key "Ctrl"]
      [:span.shortcut-key "Alt"]]))

(defmethod tool.hierarchy/help [:transform :scale]
  []
  (t [::scale [:div "Hold %1 to lock proportions, %2 to scale in place,
                     %3 to also scale children."]]
     [[:span.shortcut-key "Ctrl"]
      [:span.shortcut-key "⇧"]
      [:span.shortcut-key "Alt"]]))

(m/=> hovered? [:-> Element boolean? boolean?])
(defn hovered?
  [el intersecting?]
  (or (when-let [selection-bbox (element.hierarchy/bbox @select-box)]
        (when-let [el-bbox (:bbox el)]
          (if intersecting?
            (utils.bounds/intersect? el-bbox selection-bbox)
            (utils.bounds/contained? el-bbox selection-bbox))))
      false))

(m/=> reduce-by-area [:-> App boolean? ifn? App])
(defn reduce-by-area
  [db intersecting? f]
  (transduce
   (comp
    (element.handlers/visible)
    (filter #(hovered? % intersecting?))
    (map :id))
   (fn [db id]
     (cond-> db
       id (f id)))
   db (element.handlers/entities db)))

(defmethod tool.hierarchy/on-pointer-move :transform
  [db e]
  (let [{:keys [element]} e]
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
      (element.handlers/hover (:id element)))))

(defmethod tool.hierarchy/on-key-down :transform
  [db e]
  (cond-> db
    (= (:key e) "Shift")
    (element.handlers/ignore :bbox)

    (= (:key e) "ArrowUp")
    (element.handlers/translate [0 -1])

    (= (:key e) "ArrowDown")
    (element.handlers/translate [0 1])

    (= (:key e) "ArrowLeft")
    (element.handlers/translate [-1 0])

    (= (:key e) "ArrowRight")
    (element.handlers/translate [1 0])

    (= (:key e) "Escape")
    (history.handlers/reset-state)))

(defmethod tool.hierarchy/on-key-up :transform
  [db e]
  (cond-> db
    (= (:key e) "Shift")
    (element.handlers/clear-ignored)

    (contains? #{"ArrowUp" "ArrowDown" "ArrowLeft" "ArrowRight"} (:key e))
    (history.handlers/finalize [::move-selection "Move selection"])))

(defmethod tool.hierarchy/on-pointer-down :transform
  [db e]
  (let [{:keys [button element]} e]
    (cond-> db
      element
      (assoc :clicked-element element)

      (and (= button :right) (not= (:id element) :bbox))
      (element.handlers/toggle-selection (:id element) (:shift-key e))

      :always
      (element.handlers/ignore :bbox))))

(defmethod tool.hierarchy/on-pointer-up :transform
  [db e]
  (let [{:keys [element]} e]
    (-> db
        (dissoc :clicked-element)
        (element.handlers/unignore :bbox)
        (element.handlers/toggle-selection (:id element) (:shift-key e))
        (history.handlers/finalize (if (:selected element)
                                     [::deselect-element "Deselect element"]
                                     [::select-element "Select element"])))))

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
  (-> db
      (element.handlers/clear-ignored)
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
    (-> db
        (assoc :pivot-point pivot-point)
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

(m/=> start-point [:-> Element Vec2])
(defn start-point
  [el]
  (into [] (take 2) (:bbox el)))

(m/=> translate [:-> App Vec2 [:maybe Orientation] App])
(defn translate
  [db offset axis]
  (let [hovered-svg (element.handlers/hovered-svg db)
        user-translate? (contains? #{:translate :clone} (:state db))
        single-selection? (and (seq (element.handlers/selected db))
                               (empty? (rest (element.handlers/selected db))))
        offset (case axis
                 :vertical [(first offset) 0]
                 :horizontal [0 (second offset)]
                 offset)]
    (reduce (fn [db id]
              (let [container (element.handlers/parent-container db id)]
                (cond-> (element.handlers/translate db id offset)
                  (and single-selection?
                       user-translate?
                       (not= (:id (element.handlers/parent db id))
                             (:id hovered-svg))
                       (not (utils.element/svg? (element.handlers/entity db id))))
                  (-> (element.handlers/set-parent (:id hovered-svg))
                      ;; FIXME: Handle nested containers.
                      (cond-> (:bbox container)
                        (element.handlers/translate id (start-point container))

                        (:bbox hovered-svg)
                        (element.handlers/translate id (matrix/mul
                                                        (start-point hovered-svg)
                                                        -1)))))))
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
  (let [{:keys [clicked-element]} db
        {:keys [shift-key]} e
        {:keys [id]} clicked-element
        state (drag-start->state clicked-element)]
    (cond-> db
      :always
      (-> (tool.handlers/set-state state)
          (element.handlers/clear-hovered))

      (selectable? clicked-element)
      (-> (element.handlers/toggle-selection id shift-key)
          (snap.handlers/delete-from-tree #{id})))))

(defmethod tool.hierarchy/on-drag :transform
  [db e]
  (let [{:keys [ctrl-key alt-key shift-key]} e
        ratio-locked? (or ctrl-key (element.handlers/ratio-locked? db))
        db (element.handlers/clear-ignored db)
        delta (tool.handlers/pointer-delta db)
        axis (when ctrl-key
               (if (> (abs (first delta)) (abs (second delta)))
                 :vertical
                 :horizontal))]
    (case (:state db)
      :select
      (-> db
          (element.handlers/clear-hovered)
          (tool.handlers/add-fx [::set-select-box (select-rect db alt-key)])
          (reduce-by-area alt-key element.handlers/hover))

      :translate
      (if alt-key
        (tool.handlers/set-state db :clone)
        (-> db
            (history.handlers/reset-state)
            (select-element shift-key)
            (translate delta axis)
            (snap.handlers/snap-with translate axis)
            (tool.handlers/set-cursor "move")))

      :clone
      (if alt-key
        (-> db
            (history.handlers/reset-state)
            (select-element shift-key)
            (element.handlers/duplicate)
            (translate delta axis)
            (snap.handlers/snap-with translate axis)
            (tool.handlers/set-cursor "copy"))
        (tool.handlers/set-state db :translate))

      :scale
      (let [options {:ratio-locked ratio-locked?
                     :in-place shift-key
                     :recursive alt-key}]
        (-> db
            (history.handlers/reset-state)
            (tool.handlers/set-cursor "default")
            (scale (matrix/add delta (snap.handlers/nearest-delta db))
                   options)))

      :idle db)))

(defmethod tool.hierarchy/on-drag-end :transform
  [db e]
  (let [{:keys [state]} db]
    (cond-> db
      (= state :select)
      (-> (cond-> (not (:shift-key e)) element.handlers/deselect)
          (reduce-by-area (:alt-key e) element.handlers/select)
          (tool.handlers/add-fx [::set-select-box nil])
          (history.handlers/finalize [::modify-selection "Modify selection"]))

      (= state :translate)
      (history.handlers/finalize [::move-selection "Move selection"])

      (= state :scale)
      (history.handlers/finalize [::scale-selection "Scale selection"])

      (= state :clone)
      (history.handlers/finalize [::clone-selection "Clone selection"])

      :always
      (-> (tool.handlers/set-state :idle)
          (element.handlers/clear-hovered)
          (dissoc :clicked-element :pivot-point)))))

(defmethod tool.hierarchy/snapping-points :transform
  [db]
  (let [selected (element.handlers/selected db)
        options (-> db :snap :options)]
    (cond
      (= (:state db) :scale)
      (when-let [el (:clicked-element db)]
        [(with-meta
           (matrix/add [(:x el) (:y el)]
                       (tool.handlers/pointer-delta db))
           {:label #(t [::scale-handle "scale handle"])})])

      (not= (:state db) :idle)
      (cond-> (element.handlers/snapping-points db (filter :visible selected))
        (seq (rest selected))
        (into (utils.bounds/->snapping-points (element.handlers/bbox db) options))))))

(defmethod tool.hierarchy/snapping-elements :transform
  [db]
  (element.handlers/non-selected-visible db))

(m/=> size-label [:-> BBox any?])
(defn size-label
  [bbox]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x _min-y max-x y2] bbox
        x (+ min-x (/ (- max-x min-x) 2))
        y (+ y2 (/ (+ (/ theme.db/handle-size 2) 15) zoom))
        [w h] (utils.bounds/->dimensions bbox)
        text (str (utils.length/->fixed w 2 false)
                  " x "
                  (utils.length/->fixed h 2 false))]
    [utils.svg/label text {:x x
                           :y y}]))

(m/=> area-label [:-> number? BBox any?])
(defn area-label
  [area bbox]
  (when area
    (let [zoom @(rf/subscribe [::document.subs/zoom])
          [min-x min-y max-x] bbox
          x (+ min-x (/ (- max-x min-x) 2))
          y (+ min-y (/ (- -15 (/ theme.db/handle-size 2)) zoom))
          text (str (utils.length/->fixed area 2 false) " px²")]
      [utils.svg/label text {:x x
                             :y y}])))

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

     (when (and (pos? elements-area)
                (= state :scale)
                (seq bbox))
       [area-label elements-area bbox])

     (when (seq bbox)
       [:<>
        [tool.views/wrapping-bbox bbox]
        [tool.views/bounding-corners bbox]
        (when (= state :scale)
          [size-label bbox])])

     (when pivot-point
       [utils.svg/times pivot-point])

     [element.hierarchy/render @select-box]]))
