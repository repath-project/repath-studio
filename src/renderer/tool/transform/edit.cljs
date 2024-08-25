(ns renderer.tool.transform.edit
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
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
      (app.h/set-state :default)
      (app.h/set-cursor "default")
      (app.h/set-message
       [:<>
        [:div "Drag a handle to modify your shape, or click on an element to change selection. "]
        [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction."]])))

(defmethod tool/pointer-down :edit
  [db {:keys [element]}]
  (assoc db :clicked-element element))

(defmethod tool/pointer-up :edit
  [db {:keys [element] :as e}]
  (if-not (and (= (:button e) :right)
               (:selected? element))
    (-> db
        element.h/clear-ignored
        (dissoc :clicked-element)
        (element.h/select (:id element) (pointer/shift? e))
        (history.h/finalize "Select element"))
    (dissoc db :clicked-element)))

(defmethod tool/pointer-move :edit
  [db {:keys [element]}]
  (cond-> (element.h/clear-hovered db)
    (:id element)
    (element.h/hover (:id element))))

(defmethod tool/drag-start :edit
  [db]
  (app.h/set-state db :edit))

(defn snap-handler
  [db offset el-id handle-id]
  (element.h/update-el db el-id tool/edit offset handle-id))

(defmethod tool/drag :edit
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos clicked-element] :as db} e]
  (let [pointer-offset (mat/sub adjusted-pointer-pos adjusted-pointer-offset)
        db (history.h/swap db)
        id (:element clicked-element)
        pointer-offset (if (pointer/ctrl? e)
                         (pointer/lock-direction pointer-offset)
                         pointer-offset)]

    (cond-> db
      id
      (-> (element.h/update-el id tool/edit pointer-offset (:id clicked-element))
          (snap.h/snap snap-handler id (:id clicked-element))))))

(defmethod tool/drag-end :edit
  [db]
  (-> db
      (app.h/set-state :default)
      (dissoc :clicked-element)
      (history.h/finalize "Edit")))
