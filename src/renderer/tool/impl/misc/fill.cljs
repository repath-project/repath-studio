(ns renderer.tool.impl.misc.fill
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :fill ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :fill
  []
  {:icon "fill"})

(defmethod tool.hierarchy/help [:fill :default]
  []
  "Click on an element to fill.")

(defmethod tool.hierarchy/activate :fill
  [db]
  (app.h/set-cursor db "crosshair"))

(defmethod tool.hierarchy/pointer-up :fill
  [db e]
  (let [color (get-in db [:documents (:active-document db) :fill])
        el-id (-> e :element :id)]
    (-> db
        (element.h/set-attr el-id :fill color)
        (app.h/explain "Fill"))))

(defmethod tool.hierarchy/drag-end :fill
  [db e]
  (tool.hierarchy/pointer-up db e))
