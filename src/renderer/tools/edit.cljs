(ns renderer.tools.edit
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as elements]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.tools.base :as tools]
   [renderer.utils.mouse :as mouse]))

(derive :edit ::tools/transform)

(defmethod tools/properties :edit
  []
  {:icon "edit"})

(defmethod tools/activate :edit
  [db]
  (-> db
      (handlers/set-state :default)
      (handlers/set-message
       [:div
        [:div "Drag a handler to modify your shape, or click on an element 
              to change selection."]
        [:div "Hold " [:strong "Ctrl"] " to restrict direction."]])))

(defmethod tools/mouse-down :edit
  [db _ element]
  (assoc db :clicked-element element))

(defmethod tools/mouse-move :edit
  [db _ element]
  (-> db
      (elements/clear-hovered)
      (elements/hover (:key element))))

(defmethod tools/drag-start :edit
  [db]
  (handlers/set-state db :edit))

(defmethod tools/drag :edit
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos clicked-element] :as db} e]
  (let [mouse-offset (mat/sub adjusted-mouse-pos adjusted-mouse-offset)
        db (history/swap db)
        element-key (:element clicked-element)
        mouse-offset (if (contains? (:modifiers e) :ctrl)
                       (mouse/lock-direction mouse-offset)
                       mouse-offset)]
    (if element-key
      (assoc-in db
                (conj (elements/elements-path db) element-key)
                (tools/edit (elements/get-element db element-key)
                            mouse-offset
                            (:key clicked-element)))
      db)))

(defmethod tools/drag-end :edit
  [db]
  (-> db
      (handlers/set-state :default)
      (dissoc :clicked-element)
      (history/finalize (str "Edit " (-> db :clicked-element :key name)))))
