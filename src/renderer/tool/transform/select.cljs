
(ns renderer.tool.transform.select
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :select ::tool/tool)

(defmethod tool/properties :select
  []
  {:icon "pointer-alt"})

(defmulti message (fn [_offset state] state))

(defmethod message :default
  [_]
  [:<>
   [:div "Click or click and drag to select. "]
   [:div
    "Hold " [:span.shortcut-key "⇧"] " to add or remove elements to selection and "
    [:span.shortcut-key "Alt"] " while dragging to select intersecting elements."]])

(defmethod message :move
  [offset]
  [:<>
   [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction, and " [:span.shortcut-key "Alt"] " to clone."]
   [:div "Moving by [" (str/join " " (mapv units/->fixed offset)) "]."]])

(defmethod message :clone
  [offset]
  [:<>
   [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction. or release " [:span.shortcut-key "Alt"] " to move."]
   [:div "Cloning to [" (str/join " " (mapv units/->fixed offset)) "]."]])

(defmethod message :scale
  [ratio]
  [:<>
   [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions, " [:span.shortcut-key "⇧"] " to scale in position, " [:span.shortcut-key "Alt"] " to also scale children."]
   [:div "Scaling by [" (str/join " " (mapv units/->fixed (distinct ratio))) "]."]])

(defn hovered?
  [db el intersecting?]
  (let [{{:keys [x y width height]} :attrs} (element.h/get-temp db)
        selection-bounds [x y (+ x width) (+ y height)]]
    (when-let [el-bounds (:bounds el)]
      (if intersecting?
        (bounds/intersect? el-bounds selection-bounds)
        (bounds/contained? el-bounds selection-bounds)))))

(defn reduce-by-area
  [db intersecting? f]
  (reduce (fn [db el]
            (cond-> db
              (hovered? db el intersecting?)
              (f (:id el)))) db (filter :visible? (vals (element.h/elements db)))))

(defmethod tool/pointer-move :select
  [db {:keys [element] :as e}]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)

    :always
    (-> (element.h/clear-hovered)
        (assoc :cursor (if element "move" "default")))

    (:id element)
    (element.h/hover (:id element))))

(defmethod tool/key-down :select
  [db e]
  (cond-> db
    (pointer/shift? e)
    (element.h/ignore :bounding-box)))

(defmethod tool/key-up :select
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (element.h/clear-ignored)))

(defmethod tool/pointer-down :select
  [db {:keys [element]}]
  (cond-> db
    element
    (assoc :clicked-element element)

    :always
    (element.h/ignore :bounding-box)))

(defmethod tool/pointer-up :select
  [db {:keys [element] :as e}]
  (if-not (and (= (:button e) :right)
               (:selected? element))
    (-> db
        (dissoc :clicked-element)
        (element.h/select (:id element) (pointer/shift? e))
        (app.h/explain "Select element"))
    (dissoc db :clicked-element)))

(defmethod tool/double-click :select
  [db {:keys [element]}]
  (if (= (:tag element) :g)
    (-> db
        (element.h/ignore (:id element))
        (element.h/deselect (:id element)))
    (app.h/set-tool db :edit)))

(defmethod tool/activate :select
  [db]
  (-> db
      (app.h/set-state :default)
      (app.h/set-cursor "default")
      (app.h/set-message (message nil :default))))

(defmethod tool/deactivate :select
  [db]
  (element.h/clear-ignored db))

(defn select-rect
  [{:keys [adjusted-pointer-offset
           adjusted-pointer-pos
           active-document] :as db} intersecting?]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (cond-> (overlay/select-box adjusted-pointer-pos adjusted-pointer-offset zoom)
      (not intersecting?) (assoc-in [:attrs :fill] "transparent"))))

(defmethod tool/drag-start :select
  [db _e]
  (case (-> db :clicked-element :tag)
    :canvas
    (app.h/set-state db :select)

    :scale
    (app.h/set-state db :scale)

    (app.h/set-state db :move)))

(defn lock-ratio
  [[x y] handle]
  (let [[x y] (condp contains? handle
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(defn offset-scale
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
   □----------□--------- ■ :bottom-right (active handle)
   "
  [db [x y] lock-ratio? in-place? recur?]
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
        ratio (cond-> ratio lock-ratio? (lock-ratio handle))
        ;; TODO: Handle negative/inverted ratio.
        ratio (mapv #(max 0 %) ratio)]
    (-> db
        (assoc :pivot-point pivot-point)
        (app.h/set-message (message ratio :scale))
        (element.h/scale ratio pivot-point recur?))))

(defmethod tool/drag :select
  [{:keys [state adjusted-pointer-offset adjusted-pointer-pos] :as db} e]
  (let [offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        ctrl? (pointer/ctrl? e)
        offset (cond-> offset
                 (and ctrl? (not= state :scale))
                 (pointer/lock-direction))
        alt-key? (pointer/alt? e)
        db (-> db
               (element.h/clear-ignored)
               (app.h/set-message (message offset state)))]
    (-> (case state
          :select
          (-> db
              (element.h/set-temp (select-rect db alt-key?))
              (element.h/clear-hovered)
              (reduce-by-area (pointer/alt? e) element.h/hover))

          :move
          (if alt-key?
            (app.h/set-state db :clone)
            (-> db
                (history.h/swap)
                (cond->
                 (and (:clicked-element db) (not (-> db :clicked-element :selected?)))
                  (-> (element.h/select (-> db :clicked-element :id) (pointer/shift? e))))
                (element.h/translate offset)
                (snap.h/snap element.h/translate)
                (app.h/set-cursor "default")))

          :clone
          (if alt-key?
            (-> db
                (history.h/swap)
                (element.h/duplicate offset)
                (snap.h/snap element.h/translate)
                (app.h/set-cursor "copy"))
            (app.h/set-state db :move))

          :scale
          (cond-> db
            :always
            (-> (history.h/swap)
                (app.h/set-cursor "default")
                (offset-scale offset (pointer/ctrl? e) (pointer/shift? e) (pointer/alt? e)))

            (not (pointer/ctrl? e))
            (snap.h/snap offset-scale false (pointer/shift? e) (pointer/alt? e)))

          :default db))))

(defmethod tool/drag-end :select
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (pointer/shift? e)) element.h/deselect)
                    (reduce-by-area (pointer/alt? e) element.h/select)
                    element.h/clear-temp
                    (app.h/explain "Modify selection"))
        :move (app.h/explain db "Move selection")
        :scale (app.h/explain db "Scale selection")
        :clone (app.h/explain db "Clone selection")
        :default db)
      (app.h/set-state :default)
      (element.h/clear-hovered)
      (dissoc :clicked-element :pivot-point)
      (app.h/set-message (message nil :default))))
