
(ns renderer.tool.transform.select
  (:require
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [kdtree]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as utils.el]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :select ::tool/tool)

(defmethod tool/properties :select
  []
  {:icon "pointer-alt"})

(defmulti message (fn [_offset state] state))

(defmethod message :default
  [_]
  [:div
   [:div "Click or click and drag to select."]
   [:div
    "Hold "
    [:strong "Shift"]
    " to add or remove elements to selection and "
    [:strong "Alt"]
    " while dragging to select intersecting elements."]])

(defmethod message :move
  [offset]
  [:div
   [:div "Moving by " (str (mapv units/->fixed offset))]
   [:div
    "Hold "
    [:strong "Ctrl"]
    " to restrict direction, and "
    [:strong "Alt"] " to clone."]])

(defmethod message :clone
  [offset]
  [:div
   [:div "Cloning to " (str (mapv units/->fixed offset))]
   [:div
    "Hold "
    [:strong "Ctrl"]
    " to restrict direction. or release "
    [:strong "Alt"]
    " to move."]])

(defmethod message :scale
  [ratio]
  [:div
   [:div "Scaling by " (str (mapv units/->fixed (distinct ratio)))]
   [:div
    "Hold "
    [:strong "Ctrl"]
    " to lock proportions, and "
    [:strong "Shift"]
    " to scale in position."]])

(defn hovered?
  [db el intersecting?]
  (let [{{:keys [x y width height]} :attrs} (element.h/get-temp db)
        selection-bounds [x y (+ x width) (+ y height)]]
    (if intersecting?
      (bounds/intersected? (:bounds el) selection-bounds)
      (bounds/contained? (:bounds el) selection-bounds))))

(defn reduce-by-area
  [db intersecting? f]
  (reduce (fn [db el]
            (cond-> db
              (hovered? db el intersecting?)
              (f (:key el)))) db (filter :visible? (vals (element.h/elements db)))))

(defmethod tool/pointer-move :select
  [db {:keys [element] :as e}]
  (cond-> db
    (not (pointer/shift? e))
    element.h/clear-ignored

    :always
    (-> element.h/clear-hovered
        (element.h/hover (:key element))
        (assoc :cursor (if element "move" "default")))))

(defmethod tool/key-down :select
  [db e]
  db
  (cond-> db
    (pointer/shift? e)
    (element.h/ignore :bounding-box)))

(defmethod tool/key-up :select
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    element.h/clear-ignored))

(defmethod tool/pointer-down :select
  [db {:keys [element]}]
  (-> db
      (assoc :clicked-element element)
      (element.h/ignore :bounding-box)))

(defmethod tool/pointer-up :select
  [db {:keys [element] :as e}]
  (if-not (and (= (:button e) :right)
               (:selected? element))
    (-> db
        element.h/clear-ignored
        (dissoc :clicked-element)
        (element.h/select (:key element) (pointer/shift? e))
        (history.h/finalize "Select element"))
    (dissoc db :clicked-element)))

(defmethod tool/double-click :select
  [db {:keys [element]}]
  (if (= (:tag element) :g)
    (-> db
        (element.h/ignore (:key element))
        (element.h/deselect (:key element)))
    (h/set-tool db :edit)))

(defmethod tool/activate :select
  [db]
  (-> db
      (h/set-state :default)
      (h/set-cursor "default")
      (h/set-message (message nil :default))))

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
  [db e]
  (case (-> db :clicked-element :tag)
    :canvas
    (h/set-state db :select)

    :scale
    (h/set-state db :scale)

    :move
    (h/set-state db :move)

    (-> (cond-> db
          (and (:clicked-element db) (not (-> db :clicked-element :selected?)))
          (-> (element.h/select (-> db :clicked-element :key) (pointer/shift? e))
              (history.h/finalize "Select element")))
        (h/set-state :move))))

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
  [db [x y] lock-ratio? in-place?]
  (let [handle (-> db :clicked-element :key)
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
        (h/set-message (message ratio :scale))
        (element.h/scale ratio pivot-point))))

(defmethod tool/drag :select
  [{:keys [state adjusted-pointer-offset adjusted-pointer-pos] :as db} e]
  (let [offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        ctrl? (pointer/ctrl? e)
        offset (cond-> offset
                 (and ctrl? (not= state :scale))
                 pointer/lock-direction)
        alt-key? (contains? (:modifiers e) :alt)
        db (-> db
               element.h/clear-ignored
               (h/set-message (message offset state)))]
    (-> (case state
          :select
          (-> db
              (element.h/set-temp (select-rect db alt-key?))
              (element.h/clear-hovered)
              (reduce-by-area (contains? (:modifiers e) :alt) element.h/hover))

          :move
          (if alt-key?
            (h/set-state db :clone)
            (-> db
                history.h/swap
                (element.h/translate offset)
                (snap.h/snap element.h/translate)
                (h/set-cursor "default")))

          :clone
          (if alt-key?
            (-> db
                history.h/swap
                (element.h/duplicate offset)
                (snap.h/snap element.h/translate)
                (h/set-cursor "copy"))
            (h/set-state db :move))

          :scale
          (cond-> db
            :always
            (-> history.h/swap
                (h/set-cursor "default")
                (offset-scale offset (pointer/ctrl? e) (pointer/shift? e)))

            (not (pointer/ctrl? e))
            (snap.h/snap offset-scale false (pointer/shift? e)))

          :default db))))

(defmethod tool/drag-end :select
  [db e]
  (-> (case (:state db)
        :select (-> (cond-> db (not (pointer/shift? e)) element.h/deselect)
                    (reduce-by-area (contains? (:modifiers e) :alt) element.h/select)
                    element.h/clear-temp
                    (history.h/finalize "Modify selection"))
        :move (history.h/finalize db "Move selection")
        :scale (history.h/finalize db "Scale selection")
        :clone (history.h/finalize db "Clone selection")
        :default db)
      (h/set-state :default)
      element.h/clear-hovered
      (dissoc :clicked-element :pivot-point)
      (h/set-message (message nil :default))))
