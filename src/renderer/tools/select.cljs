
(ns renderer.tools.select
  (:require
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [renderer.element.handlers :as elements]
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
   [:div "Moving by " (str (map units/->fixed offset))]
   [:div
    "Hold "
    [:strong "Ctrl"]
    " to restrict direction, and "
    [:strong "Alt"] " to clone."]])

(defmethod message :clone
  [offset]
  [:div
   [:div "Cloning to " (str (map units/->fixed offset))]
   [:div
    "Hold "
    [:strong "Ctrl"]
    " to restrict direction. or release "
    [:strong "Alt"]
    " to move."]])

(defmethod message :scale
  [_offset]
  [:div
   [:div "Scaling"]
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
    (reduce (fn [db element]
              (if (and
                   (empty? (set/intersection (elements/ancestor-keys db element)
                                             hovered-keys))
                   (not (elements/page? element))
                   (hovered? (tools/adjusted-bounds element (elements/elements db))
                             (tools/adjusted-bounds (elements/get-temp db)
                                                    (elements/elements db))))
                (f db element)
                db))
            db
            (vals (elements/elements db)))))

(defmethod tools/mouse-move :select
  [db _e el]
  (-> db
      elements/clear-hovered
      (elements/hover (:key el))
      (assoc :cursor (if el "move" "default"))))

(defmethod tools/mouse-down :select
  [db _e el]
  (assoc db :clicked-element el))

(defmethod tools/double-click :select
  [db _e el]
  (if (= (:tag el) :g)
    (-> db
        (elements/ignore (:key el))
        (elements/deselect (:key el)))
    (tools/set-tool db :edit)))

(defmethod tools/activate :select
  [db]
  (-> db
      (handlers/set-state :default)
      (handlers/set-message (message nil :default))))

(defmethod tools/deactivate :select
  [db]
  (-> db
      elements/clear-hovered
      (elements/clear-ignored)))

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
           (elements/select (-> db :clicked-element :key) (mouse/multiselect? e))
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
   │             |       |
   │             │       │
   □ ─────────── ■ ─ x ─ □
   |          |    ↖     │
   |          y      ↖   │
   |          |        ↖ │
   □----------□--------- ■ :bottom-right (active handler)
   "     
  [db [x y] lock-ratio? in-place?]
  (let [handler (-> db :clicked-element :key)
        bounds (elements/bounds db)
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
        (elements/scale ratio pivot-point))))

(defmethod tools/drag :select
  [{:keys [state
           adjusted-pointer-offset
           adjusted-pointer-pos] :as db} e]
  (let [offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        offset (if (and (contains? (:modifiers e) :ctrl)
                        (not= state :scale))
                 (mouse/lock-direction offset)
                 offset)
        alt-key? (contains? (:modifiers e) :alt)]
    (-> (case state
          :select (-> db
                      (elements/set-temp (select-rect db alt-key?))
                      (elements/clear-hovered)
                      (reduce-by-area (contains? (:modifiers e) :alt)
                                      #(elements/hover %1 (:key %2))))

          :move (if alt-key?
                  (handlers/set-state db :clone)
                  (elements/translate (history/swap db) offset))

          :clone (if alt-key?
                   (elements/duplicate (history/swap db) offset)
                   (handlers/set-state db :move))

          :scale (offset-scale (history/swap db)
                               offset
                               (contains? (:modifiers e) :ctrl)
                               (contains? (:modifiers e) :shift))

          :default db)
        (handlers/set-message (message offset state)))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-pointer-offset] :as db} e]
  (-> (case state
        :select (-> (cond-> db (not (mouse/multiselect? e)) elements/deselect)
                    (reduce-by-area (contains? (:modifiers e) :alt)
                                    #(elements/select %1 (:key %2)))
                    (elements/clear-temp)
                    (history/finalize "Modify selection"))
        :move (history/finalize db "Move selection by " adjusted-pointer-offset)
        :scale (history/finalize db "Scale selection")
        :clone (history/finalize db "Clone selection")
        :default db)
      (handlers/set-state :default)
      (dissoc :clicked-element :pivot-point)
      (handlers/set-message (message nil :default))))
