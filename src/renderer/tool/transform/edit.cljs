(ns renderer.tool.transform.edit
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.base :as tool]
   [renderer.utils.pointer :as pointer]))

(derive :edit ::tool/tool)

(defmethod tool/properties :edit
  []
  {:icon "edit"})

(defmethod tool/activate :edit
  [db]
  (-> db
      (h/set-state :default)
      (h/set-cursor "default")
      (h/set-message
       [:div
        [:div "Drag a handle to modify your shape, or click on an element
              to change selection."]
        [:div "Hold " [:strong "Ctrl"] " to restrict direction."]])))

(defmethod tool/pointer-down :edit
  [db {:keys [element]}]
  (assoc db :clicked-element element))

(defmethod tool/pointer-up :edit
  [db {:keys [element] :as e}]
  (if-not (and (= (:button e) :right)
               (:selected? element))
    (-> db
        (dissoc :clicked-element)
        (element.h/select (:key element) (pointer/shift? e))
        (history.h/finalize "Select element"))
    (dissoc db :clicked-element)))

(defmethod tool/pointer-move :edit
  [db {:keys [element]}]
  (-> db
      element.h/clear-hovered
      (element.h/hover (:key element))))

(defmethod tool/drag-start :edit
  [db]
  (h/set-state db :edit))

(defn snap-handler
  [db offset el k]
  (element.h/update-el db el tool/edit offset k))

(defmethod tool/drag :edit
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos clicked-element] :as db} e]
  (let [pointer-offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        db (history.h/swap db)
        el-key (:element clicked-element)
        el (element.h/element db el-key)
        pointer-offset (if (pointer/ctrl? e)
                         (pointer/lock-direction pointer-offset)
                         pointer-offset)]

    (cond-> db
      el-key
      (-> (element.h/update-el el tool/edit pointer-offset (:key clicked-element))
          (snap.h/snap snap-handler el (:key clicked-element))))))

(defmethod tool/drag-end :edit
  [db]
  (-> db
      (h/set-state :default)
      (dissoc :clicked-element)
      (history.h/finalize "Edit " (-> db :clicked-element :key name))))
