
(ns renderer.tools.select
  (:require [renderer.tools.base :as tools]
            [renderer.elements.handlers :as elements]
            [renderer.overlay :as overlay]
            [renderer.utils.bounds :as bounds]
            [renderer.utils.units :as units]
            [clojure.core.matrix :as matrix]
            [clojure.set :as set]
            [renderer.history.handlers :as history]
            [renderer.handlers :as handlers]
            [renderer.utils.mouse :as mouse]))

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
    [:strong "Alt"] 
    " to center the pivot point."]])

(defn reduce-by-area
  [{:keys [active-document] :as db} intersecting? func]
  (let [predicate (if intersecting? bounds/intersect? bounds/contained?)
        hovered-keys (-> db :documents active-document :hovered-keys)]
    (reduce #(if (and
                  (empty? (set/intersection (elements/ancestor-keys db %2)
                                            hovered-keys))
                  (not (elements/page? %2))
                  (predicate (tools/adjusted-bounds %2 (elements/elements db))
                             (tools/adjusted-bounds (elements/get-temp db)
                                                    (elements/elements db))))
               (func %1 %2)
               %)
            db
            (vals (elements/elements db)))))

(defmethod tools/mouse-move :select
  [db _ element]
  (-> db
      (elements/clear-hovered)
      (elements/hover (:key element))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/mouse-down :select
  [db _ element]
  (assoc db :clicked-element element))

(defmethod tools/double-click :select
  [db _event element]
  (if (= (:tag element) :g)
    (-> db
        (elements/ignore (:key element))
        (elements/deselect-element (:key element)))
    (tools/set-tool db :edit)))

(defmethod tools/activate :select
  [db]
  (-> db
      (handlers/set-state :default)
      (handlers/set-message (message nil :default))))

(defmethod tools/deactivate :select
  [db]
  (-> db
      (elements/clear-hovered)
      (elements/clear-ignored)))

(defn select-rect
  [{:keys [adjusted-mouse-offset 
           adjusted-mouse-pos 
           active-document] :as db} intersecting?]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (cond-> (overlay/select-box adjusted-mouse-pos adjusted-mouse-offset zoom)
      (not intersecting?) (assoc-in [:attrs :fill] "transparent"))))

(defmethod tools/drag-start :select
  [db event]
  (case (-> db :clicked-element :tag)
    :canvas 
    (handlers/set-state db :select)

    :scale 
    (handlers/set-state db :scale)
    
    (handlers/set-state
     (if-not (-> db :clicked-element :selected?)
       (-> db
           (elements/select (mouse/multiselect? event) (:clicked-element db))
           (history/finalize "Select element"))
       db) :move)))

(defmethod tools/drag :select
  [{:keys [state 
           adjusted-mouse-offset 
           adjusted-mouse-pos] :as db} event]
  (let [offset (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)
        offset (if (and (contains? (:modifiers event) :ctrl)
                        (not= state :scale)) 
                 (mouse/lock-direction offset) 
                 offset)
        alt-key? (contains? (:modifiers event) :alt)]
    (-> (case state
          :select
          (-> db
              (elements/set-temp (select-rect db alt-key?))
              (elements/clear-hovered)
              (reduce-by-area (contains? (:modifiers event) :alt)
                              #(elements/hover %1 (:key %2))))

          :move
          (if alt-key?
            (handlers/set-state db :clone)
            (elements/translate (history/swap db) offset))

          :clone
          (if alt-key?
            (elements/duplicate (history/swap db) offset)
            (handlers/set-state db :move))

          :scale
          (elements/scale (history/swap db) offset
                          (contains? (:modifiers event) :ctrl)
                          (contains? (:modifiers event) :shift))
          
          :default db)
        (handlers/set-message (message offset state)))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-mouse-offset] :as db} event]
  (-> (case state
        :select (-> (if (not (mouse/multiselect? event))
                      (elements/deselect-all db)
                      db)
                    (reduce-by-area (contains? (:modifiers event) :alt)
                                    #(elements/select-element % (:key %2)))
                    (elements/clear-temp)
                    (history/finalize "Modify selection"))
        :move (history/finalize db (str "Move selection by " adjusted-mouse-offset))
        :scale (history/finalize db "Scale selection")
        :clone (history/finalize db "Clone selection")
        :default db)
      (handlers/set-state :default)
      (dissoc :clicked-element)
      (handlers/set-message (message nil :default))))