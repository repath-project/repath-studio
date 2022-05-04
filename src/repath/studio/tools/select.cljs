
(ns repath.studio.tools.select
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.styles :as styles]
            [repath.studio.bounds :as bounds]
            [clojure.core.matrix :as matrix]
            [clojure.set :as set]
            [repath.studio.history.handlers :as history]))

(derive :select ::tools/transform)

(defmethod tools/properties :select [] {:icon "pointer"})

(defn hover-by-area
  [db intersecting?]
  (let [active-document (:active-document db)
        predicate (if intersecting? bounds/intersect? bounds/contained?)
        hovered-keys (-> db :documents active-document :hovered-keys)
        db (assoc-in db [:documents active-document :hovered-keys] #{})]
    (reduce #(if (and
                  (empty? (set/intersection (elements/ancestor-keys db %2) hovered-keys))
                  (not (elements/page? %2))
                  (predicate (tools/adjusted-bounds %2 (elements/elements db)) (tools/adjusted-bounds (elements/get-temp db) (elements/elements db))))
               (update-in % [:documents active-document :hovered-keys] conj (:key %2))
               %) db (vals (elements/elements db)))))

(defn select-by-area
  [db intersecting? multiselect?]
  (let [active-document (:active-document db)
        predicate (if intersecting? bounds/intersect? bounds/contained?)
        hovered-keys (-> db :documents active-document :hovered-keys)]
    (reduce #(if (and
                  (empty? (set/intersection (elements/ancestor-keys db %2) hovered-keys))
                  (not (elements/page? %2))
                  (predicate (tools/adjusted-bounds %2 (elements/elements db)) (tools/adjusted-bounds (elements/get-temp db) (elements/elements db))))
               (elements/select-element % (:key %2))
               %) (cond-> db
                    (not multiselect?) (elements/deselect-all)
                    :always (elements/clear-temp)) (vals (elements/elements db)))))

(defmethod tools/mouse-move :select
  [{active-document :active-document :as db} _ element]
  (-> db
      (assoc-in [:documents active-document :hovered-keys] (if element #{(:key element)} #{}))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/mouse-down :select
  [db _ element]
  (assoc db :clicked-element element))

(defmethod tools/activate :select
  [db]
  (assoc db 
         :cursor "default"
         :state :default))

(defmethod tools/drag :select
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos active-document] :as db} event element]
  (let [offset (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)]
    (case state
      :select (let [zoom (get-in db [:documents active-document :zoom])
                    [offset-x _] adjusted-mouse-offset
                    [pos-x _] adjusted-mouse-pos
                    intersecting? (> pos-x offset-x)
                    temp-element (assoc-in (element-views/select-box adjusted-mouse-pos adjusted-mouse-offset zoom) [:attrs :fill] (if intersecting? styles/accent "transparent"))]
                (-> db
                    (elements/set-temp temp-element)
                    (hover-by-area intersecting?)))
      :translate (if (contains? (:modifiers event) :ctrl)
                   (assoc db
                          :state :clone
                          :cursor "copy")
                   (elements/translate (history/swap db) offset))
      :clone (elements/duplicate (history/swap db) offset)
      :scale (elements/scale (history/swap db) offset (:scale db) (contains? (:modifiers event) :ctrl))
      :default (assoc (case (-> db :clicked-element :type)
                        :canvas (assoc db :state :select)
                        :scale-handler (assoc db
                                              :state :scale
                                              :scale (:key element))

                        (assoc (if-not (-> db :clicked-element :selected?)
                                 (-> db
                                     (elements/select (contains? (:modifiers event) :shift) (:clicked-element db))
                                     (history/finalize "Select element"))
                                 db) :state :translate)) :cursor "default"))))

(defmethod tools/drag-end :select
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos] :as db} event]
  (let [[offset-x _] adjusted-mouse-offset
        [pos-x _] adjusted-mouse-pos]
    (assoc (case state
             :select (-> db
                         (select-by-area (> pos-x offset-x) (some #(contains? (:modifiers event) %) #{:ctrl :shift}))
                         (history/finalize "Modify selection"))
             :translate (history/finalize db (str "Move selection by " adjusted-mouse-offset))
             :scale (history/finalize db "Scale selection")
             :clone (history/finalize db "Clone selection")
             :default db)
           :cursor "default"
           :state :default)))
