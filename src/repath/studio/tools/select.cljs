
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
        temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (-> db
        (assoc-in [:documents (:active-document db) :hovered-keys] #{})
        (elements/conj-by-bounds-overlap (if intersecting? bounds/intersect? bounds/contained?) [:documents active-document :hovered-keys] temp-element))))

(defn select-by-area
  [db intersecting? multiselect?]
  (let [active-document (:active-document db)
        temp-element (get-in db [:documents active-document :temp-element])]
    (cond-> db
        (not multiselect?) (elements/deselect-all)
        :always (-> (elements/clear-temp)
                    (elements/conj-by-bounds-overlap (if intersecting? bounds/intersect? bounds/contained?) [:documents active-document :selected-keys] temp-element)))))

(defmethod tools/mouse-move :select
  [{active-document :active-document :as db} _ element]
  (-> db
      (assoc-in [:documents active-document :hovered-keys] (if element #{(:key element)} #{}))
      (assoc :cursor (if element "move" "default"))))

(defmethod tools/activate :select
  [db _ _]
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
    (let [selected-keys (get-in db [:documents active-document :selected-keys])
          offset (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)
          is-element-selected? (contains? selected-keys (:key element))]
      (case state
        :translate (elements/translate (if (some #(contains? (:modifiers event) %) #{:ctrl})
                                         (-> db
                                             (assoc :state :clone
                                                    :cursor "copy")
                                             (elements/duplicate))
                                         (history/swap db)) offset)
        :clone (elements/translate db (:adjusted-mouse-diff db))
        :scale (elements/scale (history/swap db) offset (:scale db) (contains? (:modifiers event) :ctrl))
        (cond-> db
          (not (or is-element-selected? (= (:type element) :scale-handler))) (elements/select (contains? (:modifiers event) :shift) element)
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
             :select (select-by-area db (> pos-x offset-x) (some #(contains? (:modifiers event) %) #{:ctrl :shift}))
             :translate (history/finalize db (str "Move selection by " adjusted-mouse-offset))
             :scale (history/finalize db "Scale selection")
             :clone (history/finalize db "Clone selection")
             db)
           :cursor "default"
           :state :default)))
