(ns renderer.tools.edit
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.tools.base :as tools]
   [renderer.utils.pointer :as pointer]))

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

(defmethod tools/pointer-down :edit
  [db _ el]
  (assoc db :clicked-element el))

(defmethod tools/pointer-move :edit
  [db _ el]
  (-> db
      element.h/clear-hovered
      (element.h/hover (:key el))))

(defmethod tools/drag-start :edit
  [db]
  (handlers/set-state db :edit))

(defmethod tools/drag :edit
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos clicked-element] :as db} e]
  (let [pointer-offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        db (history/swap db)
        element-key (:element clicked-element)
        pointer-offset (if (contains? (:modifiers e) :ctrl)
                         (pointer/lock-direction pointer-offset)
                         pointer-offset)]
    (if element-key
      (assoc-in db
                (conj (element.h/path db) element-key)
                (tools/edit (element.h/element db element-key)
                            pointer-offset
                            (:key clicked-element)))
      db)))

(defmethod tools/drag-end :edit
  [db]
  (-> db
      (handlers/set-state :default)
      (dissoc :clicked-element)
      (history/finalize "Edit " (-> db :clicked-element :key name))))
