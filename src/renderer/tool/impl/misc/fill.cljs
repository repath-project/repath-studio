(ns renderer.tool.impl.misc.fill
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.i18n :refer [t]]))

(derive :fill ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :fill
  []
  {:icon "fill"
   :label (t [::label "Fill"])})

(defmethod tool.hierarchy/help [:fill :idle]
  []
  (t [::help "Click on an element to fill."]))

(defmethod tool.hierarchy/on-activate :fill
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/on-pointer-up :fill
  [db e]
  (let [color (document.handlers/attr db :fill)
        el-id (-> e :element :id)]
    (-> (element.handlers/set-attr db el-id :fill color)
        (history.handlers/finalize #(t [::fill "Fill"])))))

(defmethod tool.hierarchy/on-pointer-move :fill
  [db e]
  (let [color (document.handlers/attr db :fill)
        el (-> e :element)]
    (cond-> (history.handlers/reset-state db)
      (not (utils.element/root? el))
      (element.handlers/set-attr (:id el) :fill color))))

(defmethod tool.hierarchy/on-pointer-down :fill
  [db _e]
  (history.handlers/reset-state db))
