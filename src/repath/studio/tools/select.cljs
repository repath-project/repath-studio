
(ns repath.studio.tools.select
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.styles :as styles]
            [repath.studio.bounds :as bounds]
            [clojure.core.matrix :as matrix]
            [clojure.set :as set]
            [repath.studio.history.handlers :as history]
            [repath.studio.handlers :as handlers]
            [repath.studio.mouse :as mouse]))

(derive :select ::tools/transform)

(defmethod tools/properties :select [] {:icon "pointer"})

(defn intersecting?
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos]}]
  (let [[offset-x _] adjusted-mouse-offset
        [pos-x _] adjusted-mouse-pos]
    (> pos-x offset-x)))

(defn reduce-by-area
  [{:keys [active-document] :as db} func]
  (let [predicate (if (intersecting? db) bounds/intersect? bounds/contained?)
        hovered-keys (-> db :documents active-document :hovered-keys)]
    (reduce #(if (and
                  (empty? (set/intersection (elements/ancestor-keys db %2) hovered-keys))
                  (not (elements/page? %2))
                  (predicate (tools/adjusted-bounds %2 (elements/elements db)) (tools/adjusted-bounds (elements/get-temp db) (elements/elements db))))
               (func %1 %2)
               %) db (vals (elements/elements db)))))

(defmethod tools/mouse-move :select
  [db _ element]
  (-> db
      (elements/clear-hovered)
      (elements/hover (:key element))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/mouse-down :select
  [db _ element]
  (assoc db :clicked-element element))

(defmethod tools/activate :select
  [db]
  (handlers/set-state db :default))

(defn select-rect
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos active-document] :as db}]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (assoc-in (element-views/select-box adjusted-mouse-pos adjusted-mouse-offset zoom) [:attrs :fill] (if (intersecting? db) styles/accent "transparent"))))

(defmethod tools/drag-start :select
  [db event element]
  (case (-> db :clicked-element :type)
    :canvas (handlers/set-state db :select)
    :scale-handler (-> db
                       (handlers/set-state :scale)
                       (assoc :scale (:key element)))
    (handlers/set-state (if-not (-> db :clicked-element :selected?)
                          (-> db
                              (elements/select (mouse/multiselect? event) (:clicked-element db))
                              (history/finalize "Select element"))
                          db) :translate)))

(defmethod tools/drag :select
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos] :as db} event]
  (let [offset (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)]
    (case state
      :select (-> db
                  (elements/set-temp (select-rect db))
                  (elements/clear-hovered)
                  (reduce-by-area #(elements/hover %1 (:key %2))))
      :translate (if (contains? (:modifiers event) :ctrl)
                   (handlers/set-state db :clone)
                   (elements/translate (history/swap db) offset))
      :clone (elements/duplicate (history/swap db) offset)
      :scale (elements/scale (history/swap db) offset (:scale db) (contains? (:modifiers event) :ctrl)))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-mouse-offset] :as db} event]
  (handlers/set-state (case state
               :select (-> (if (not (mouse/multiselect? event)) (elements/deselect-all db) db)
                           (reduce-by-area #(elements/select-element % (:key %2)))
                           (elements/clear-temp)
                           (history/finalize "Modify selection"))
               :translate (history/finalize db (str "Move selection by " adjusted-mouse-offset))
               :scale (history/finalize db "Scale selection")
               :clone (history/finalize db "Clone selection")
               :default db) :default))
