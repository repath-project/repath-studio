(ns renderer.tool.impl.base.edit
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.pointer :as pointer]))

(derive :edit ::hierarchy/tool)

(defmethod hierarchy/properties :edit
  []
  {:icon "edit"})

(defmethod hierarchy/help [:edit :idle]
  []
  "Drag a handle to modify your shape, or click on an element to change selection.")

(defmethod hierarchy/help [:edit :edit]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to restrict direction."])

(defmethod hierarchy/activate :edit
  [db]
  (-> db
      (h/set-state :idle)
      (h/set-cursor "default")))

(defmethod hierarchy/pointer-down :edit
  [db e]
  (cond-> db
    (:element e)
    (assoc :clicked-element (:element e))))

(defmethod hierarchy/pointer-up :edit
  [db e]
  (if-not (and (= (:button e) :right)
               (:selected (:element e)))
    (-> db
        (element.h/clear-ignored)
        (dissoc :clicked-element)
        (element.h/select (-> e :element :id) (pointer/shift? e))
        (h/explain "Select element"))
    (dissoc db :clicked-element)))

(defmethod hierarchy/pointer-move :edit
  [db e]
  (let [el-id (-> e :element :id)]
    (cond-> (element.h/clear-hovered db)
      el-id
      (element.h/hover el-id))))

(defmethod hierarchy/drag-start :edit
  [db]
  (h/set-state db :edit))

(defn snap-handler
  [db offset el-id handle-id]
  (element.h/update-el db el-id element.hierarchy/edit offset handle-id))

(defmethod hierarchy/drag :edit
  [db e]
  (let [clicked-element (:clicked-element db)
        db (history.h/swap db)
        el-id (:element clicked-element)
        delta (cond-> (h/pointer-delta db) (pointer/ctrl? e) pointer/lock-direction)]
    (cond-> db
      el-id
      (-> (element.h/update-el el-id element.hierarchy/edit delta (:id clicked-element))
          (snap.h/snap-with snap-handler el-id (:id clicked-element))))))

(defmethod hierarchy/drag-end :edit
  [db _e]
  (-> db
      (h/set-state :idle)
      (dissoc :clicked-element)
      (h/explain "Edit")))
