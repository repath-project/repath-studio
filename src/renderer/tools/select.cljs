
(ns renderer.tools.select
  (:require
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.overlay :as overlay]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.mouse :as mouse]
   [renderer.utils.units :as units]))

(derive :select ::tools/transform)

(defmethod tools/properties :select
  []
  {:icon "pointer"})

(defmulti message (fn [_offset state] state))

(defmethod message :default
  [_]
  [:div
   [:div "Click or click and drag to select."]
   [:div
    "Hold "
    [:strong "Ctrl"]
    " to add elements to selection or remove them, and "
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

(defn reduce-by-area
  [{:keys [active-document] :as db} intersecting? f]
  (let [hovered? (if intersecting? bounds/intersect-bounds? bounds/contain-bounds?)
        hovered-keys (-> db :documents active-document :hovered-keys)]
    (reduce (fn [db el]
              (if (and (empty? (set/intersection (element.h/ancestor-keys db el) hovered-keys))
                       (not (element.h/page? el))
                       (hovered? (tools/adjusted-bounds el (element.h/elements db))
                                 (tools/adjusted-bounds (element.h/get-temp db)
                                                        (element.h/elements db))))
                (f db (:key el))
                db))
            db
            (vals (element.h/elements db)))))

(defmethod tools/mouse-move :select
  [db _e el]
  (-> db
      element.h/clear-hovered
      (element.h/hover (:key el))
      (assoc :cursor (if el "move" "default"))))

(defmethod tools/mouse-down :select
  [db _e el]
  (assoc db :clicked-element el))

(defmethod tools/double-click :select
  [db _e el]
  (if (= (:tag el) :g)
    (-> db
        (element.h/ignore (:key el))
        (element.h/deselect (:key el)))
    (tools/set-tool db :edit)))

(defmethod tools/activate :select
  [db]
  (-> db
      (handlers/set-state :default)
      (handlers/set-message (message nil :default))))

(defmethod tools/deactivate :select
  [db]
  (-> db
      element.h/clear-hovered
      (element.h/clear-ignored)))

(defn select-rect
  [{:keys [adjusted-pointer-offset
           adjusted-pointer-pos
           active-document] :as db} intersecting?]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (cond-> (overlay/select-box adjusted-pointer-pos adjusted-pointer-offset zoom)
      (not intersecting?) (assoc-in [:attrs :fill] "transparent"))))

(defmethod tools/drag-start :select
  [db e]
  (case (-> db :clicked-element :tag)
    :canvas
    (handlers/set-state db :select)

    :scale
    (handlers/set-state db :scale)

    (handlers/set-state
     (if-not (-> db :clicked-element :selected?)
       (-> db
           (element.h/select (-> db :clicked-element :key) (mouse/multiselect? e))
           (history/finalize "Select element"))
       db) :move)))

(defn lock-ratio
  [[x y] handler]
  (let [[x y] (condp contains? handler
                #{:middle-right :middle-left} [x x]
                #{:top-middle :bottom-middle} [y y]
                [x y])
        ratio (if (< (abs x) (abs y)) x y)]
    [ratio ratio]))

(defn offset-scale
  "Translates the x/y offset and the handler to a ratio and a pivot point,
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
   □----------□--------- ■ :bottom-right (active handler)
   "
  [db [x y] lock-ratio? in-place?]
  (let [handler (-> db :clicked-element :key)
        bounds (element.h/bounds db)
        dimensions (bounds/->dimensions bounds)
        [x1 y1 x2 y2] bounds
        [cx cy] (bounds/center bounds)
        [offset pivot-point] (case handler
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
        ratio (cond-> ratio lock-ratio? (lock-ratio handler))
        ;; TODO: Handle negative/inverted ratio.
        ratio (mapv #(max 0 %) ratio)]
    (-> db
        (assoc :pivot-point pivot-point)
        (handlers/set-message (message ratio :scale))
        (element.h/scale ratio pivot-point))))

(defmethod tools/drag :select
  [{:keys [state
           adjusted-pointer-offset
           adjusted-pointer-pos] :as db} e]
  (let [offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        offset (if (and (contains? (:modifiers e) :ctrl)
                        (not= state :scale))
                 (mouse/lock-direction offset)
                 offset)
        alt-key? (contains? (:modifiers e) :alt)
        db (handlers/set-message db (message offset state))]
    (-> (case state
          :select
          (-> db
              (element.h/set-temp (select-rect db alt-key?))
              (element.h/clear-hovered)
              (reduce-by-area (contains? (:modifiers e) :alt) element.h/hover))

          :move
          (if alt-key?
            (handlers/set-state db :clone)
            (element.h/translate (history/swap db) offset))

          :clone
          (if alt-key?
            (element.h/duplicate (history/swap db) offset)
            (handlers/set-state db :move))

          :scale
          (offset-scale (history/swap db)
                        offset
                        (contains? (:modifiers e) :ctrl)
                        (contains? (:modifiers e) :shift))

          :default db))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-pointer-offset] :as db} e]
  (-> (case state
        :select (-> (cond-> db (not (mouse/multiselect? e)) element.h/deselect)
                    (reduce-by-area (contains? (:modifiers e) :alt) element.h/select)
                    (element.h/clear-temp)
                    (history/finalize "Modify selection"))
        :move (history/finalize db "Move selection by " adjusted-pointer-offset)
        :scale (history/finalize db "Scale selection")
        :clone (history/finalize db "Clone selection")
        :default db)
      (handlers/set-state :default)
      (dissoc :clicked-element :pivot-point)
      (handlers/set-message (message nil :default))))
