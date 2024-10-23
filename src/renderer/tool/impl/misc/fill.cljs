(ns renderer.tool.impl.misc.fill
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :fill ::hierarchy/tool)

(defmethod hierarchy/properties :fill
  []
  {:icon "fill"})

(defmethod hierarchy/help [:fill :idle]
  []
  "Click on an element to fill.")

(defmethod hierarchy/activate :fill
  [db]
  (h/set-cursor db "crosshair"))

(defmethod hierarchy/pointer-up :fill
  [db e]
  (let [color (get-in db [:documents (:active-document db) :fill])
        el-id (-> e :element :id)]
    (-> db
        (element.h/set-attr el-id :fill color)
        (h/explain "Fill"))))

(defmethod hierarchy/drag-end :fill
  [db e]
  (hierarchy/pointer-up db e))
