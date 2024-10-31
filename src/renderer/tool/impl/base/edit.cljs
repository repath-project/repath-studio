(ns renderer.tool.impl.base.edit
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.handlers :as element.h]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.history.handlers :as history.h]
   [renderer.snap.handlers :as snap.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.overlay :as overlay]
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
        (history.h/finalize "Select element"))
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

(defmethod hierarchy/drag :edit
  [db e]
  (let [clicked-element (:clicked-element db)
        db (history.h/swap db)
        el-id (:element clicked-element)
        handle-id (:id clicked-element)
        delta (cond-> (mat/add (h/pointer-delta db) (snap.h/nearest-delta db))
                (pointer/ctrl? e)
                (pointer/lock-direction))]
    (cond-> db
      el-id
      (-> (element.h/update-el el-id element.hierarchy/edit delta handle-id)))))

(defmethod hierarchy/drag-end :edit
  [db _e]
  (-> db
      (h/set-state :idle)
      (dissoc :clicked-element)
      (snap.h/update-tree)
      (history.h/finalize "Edit")))

(defmethod hierarchy/snapping-bases :edit
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (mat/add [(:x el) (:y el)]
                (h/pointer-delta db))
       {:label (when (= (:type el) :handle)
                 (or (:label el)
                     (name (:id el))))})]))

(defmethod hierarchy/snapping-points :edit
  [db]
  (let [visible-elements (filter :visible (vals (element.h/entities db)))]
    (element.h/snapping-points db visible-elements)))

(defmethod hierarchy/render :edit
  []
  (let [selected-elements @(rf/subscribe [::element.s/selected])]
    [:<>
     (for [el selected-elements]
       ^{:key (str (:id el) "-edit-points")}
       [:g
        (element.hierarchy/render-edit el)
        ^{:key (str (:id el) "-centroid")}
        [overlay/centroid el]])]))
