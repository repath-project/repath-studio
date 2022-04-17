
(ns repath.studio.tools.select
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.styles :as styles]
            [repath.studio.bounds :as bounds]
            [clojure.core.matrix :as matrix]
            [repath.studio.history.handlers :as history]))

(derive :select ::tools/transform)

(defmethod tools/properties :select [] {:icon "pointer"})

(defn hover-by-area
  [db intersecting?]
  (let [active-document (:active-document db)
        predicate (if intersecting? bounds/intersect? bounds/contained?)
        db (assoc-in db [:documents active-document :hovered-keys] #{})]
    (reduce #(if (and (not (elements/page? %2)) (predicate (tools/adjusted-bounds %2 (elements/elements db)) (tools/adjusted-bounds (elements/get-temp db) (elements/elements db))))
               (update-in % [:documents active-document :hovered-keys] conj (:key %2))
               %) db (vals (elements/elements db)))))

(defn select-by-area
  [db intersecting? multiselect?]
  (let [predicate (if intersecting? bounds/intersect? bounds/contained?)]
    (reduce #(if (and (not (elements/page? %2)) (predicate (tools/adjusted-bounds %2 (elements/elements db)) (tools/adjusted-bounds (elements/get-temp db) (elements/elements db))))
               (elements/select-element % (:key %2))
               %) (cond-> db
                    (not multiselect?) (elements/deselect-all)
                    :always (elements/clear-temp)) (vals (elements/elements db)))))

(defmethod tools/mouse-move :select
  [{active-document :active-document :as db} _ element]
  (-> db
      (assoc-in [:documents active-document :hovered-keys] (if element #{(:key element)} #{}))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/activate :select
  [db]
  (assoc db 
         :cursor "default"
         :state :default))

(defmethod tools/drag :select
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos active-document] :as db} event element]
  (if (or (and (not element) (= state :default)) (= state :select))
    (let [zoom (get-in db [:documents active-document :zoom])
          [offset-x _] adjusted-mouse-offset
          [pos-x _] adjusted-mouse-pos
          intersecting? (> pos-x offset-x)
          temp-element (assoc-in (element-views/select-box adjusted-mouse-pos adjusted-mouse-offset zoom) [:attrs :fill] (if intersecting? styles/accent "transparent"))]
      (-> db
          (assoc :state :select)
          (elements/set-temp temp-element)
          (hover-by-area intersecting?)))
    (let [offset (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)]
      (case state
        :translate (if (some #(contains? (:modifiers event) %) #{:ctrl})
                     (assoc db
                            :state :clone
                            :cursor "copy")
                     (elements/translate (history/swap db) offset))
        :clone (elements/duplicate (history/swap db) offset)
        :scale (elements/scale (history/swap db) offset (:scale db) (contains? (:modifiers event) :ctrl))
        (cond-> db
          (not (or (:selected? element) (= (:type element) :scale-handler))) (->
                                                                              (elements/select (contains? (:modifiers event) :shift) element)
                                                                              (history/finalize "Select element"))
          (not= (:type element) :scale-handler) (assoc :cursor "default"
                                                        :state :translate)
          (= (:type element) :scale-handler) (assoc :state :scale
                                                     :cursor "default"
                                                     :scale (:key element)))))))

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
             db)
           :cursor "default"
           :state :default)))
