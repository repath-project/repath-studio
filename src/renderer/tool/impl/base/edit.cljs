(ns renderer.tool.impl.base.edit
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.history.handlers :as history.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.svg :as utils.svg]))

(derive :edit ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :edit
  []
  {:icon "edit"
   :label (t [::label "Edit"])})

(defmethod tool.hierarchy/help [:edit :idle]
  []
  [:<>
   (t [::help-idle-drag "Drag a handle to modify your shape."])
   (t [::help-idle-click "Click on an element to change selection"])])

(defmethod tool.hierarchy/help [:edit :edit]
  []
  (t [::help-edit "Hold %1 to restrict direction."]
     [[:span.shortcut-key "Ctrl"]]))

(defmethod tool.hierarchy/help [:edit :type]
  []
  (t [::help-type "Enter your text."]))

(defmethod tool.hierarchy/on-pointer-down :edit
  [db e]
  (cond-> db
    (:element e)
    (assoc :clicked-element (:element e))))

(defmethod tool.hierarchy/on-pointer-up :edit
  [db e]
  (if-not (and (= (:button e) :right)
               (:selected (:element e)))
    (-> (element.handlers/clear-ignored db)
        (dissoc :clicked-element)
        (element.handlers/toggle-selection (-> e :element :id) (:shift-key e))
        (history.handlers/finalize #(t [::select-element "Select element"])))
    (dissoc db :clicked-element)))

(defmethod tool.hierarchy/on-pointer-move :edit
  [db e]
  (let [el-id (-> e :element :id)]
    (cond-> (element.handlers/clear-hovered db)
      el-id
      (element.handlers/hover el-id))))

(defmethod tool.hierarchy/on-drag-start :edit
  [db e]
  (cond-> db
    (= (-> e :element :type) :handle)
    (tool.handlers/set-state :edit)))

(defn lock-direction
  "Locks pointer movement to the axis with the biggest offset"
  [[x y]]
  (if (> (abs x) (abs y))
    [x 0]
    [0 y]))

(defmethod tool.hierarchy/on-drag :edit
  [db e]
  (let [clicked-element (:clicked-element db)
        db (history.handlers/reset-state db)
        el-id (:element clicked-element)
        handle-id (:id clicked-element)
        delta (cond-> (matrix/add (tool.handlers/pointer-delta db)
                                  (snap.handlers/nearest-delta db))
                (:ctrl-key e)
                (lock-direction))]
    (cond-> db
      el-id
      (element.handlers/update-el el-id element.hierarchy/edit delta handle-id))))

(defmethod tool.hierarchy/on-drag-end :edit
  [db _e]
  (-> (tool.handlers/set-state db :idle)
      (dissoc :clicked-element)
      (history.handlers/finalize #(t [::edit "Edit"]))))

(defmethod tool.hierarchy/snapping-points :edit
  [db]
  (when-let [el (:clicked-element db)]
    [(with-meta
       (matrix/add [(:x el) (:y el)]
                   (tool.handlers/pointer-delta db))
       {:label (when (= (:type el) :handle)
                 (or (:label el)
                     (name (:id el))))})]))

(defmethod tool.hierarchy/snapping-elements :edit
  [db]
  (element.handlers/non-selected-visible db))

(defmethod tool.hierarchy/render :edit
  []
  (let [selected-elements @(rf/subscribe [::element.subs/selected])]
    [:g
     (for [el selected-elements]
       ^{:key (str (:id el) "-edit-points")}
       [:g
        [element.hierarchy/render-edit el]
        (when-let [pos (element.hierarchy/centroid el)]
          (let [offset (utils.element/offset el)
                pos (matrix/add offset pos)]
            ^{:key (str (:id el) "-centroid")}
            [utils.svg/dot pos [:title (t [::centroid "Centroid"])]]))])]))
