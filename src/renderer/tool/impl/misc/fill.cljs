(ns renderer.tool.impl.misc.fill
  (:require
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :fill ::hierarchy/tool)

(defmethod hierarchy/properties :fill
  []
  {:icon "fill"})

(defmethod hierarchy/help [:fill :idle]
  []
  "Click on an element to fill.")

(defmethod hierarchy/on-activate :fill
  [db]
  (h/set-cursor db "crosshair"))

(defmethod hierarchy/on-pointer-up :fill
  [db e]
  (let [color (document.h/attr db :fill)
        el-id (-> e :element :id)]
    (-> (element.h/set-attr db el-id :fill color)
        (history.h/finalize "Fill"))))

