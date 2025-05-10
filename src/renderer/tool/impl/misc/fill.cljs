(ns renderer.tool.impl.misc.fill
  (:require
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :fill ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :fill
  []
  {:icon "fill"})

(defmethod tool.hierarchy/help [:fill :idle]
  []
  "Click on an element to fill.")

(defmethod tool.hierarchy/on-activate :fill
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/on-pointer-up :fill
  [db e]
  (let [color (document.handlers/attr db :fill)
        el-id (-> e :element :id)]
    (-> (element.handlers/set-attr db el-id :fill color)
        (history.handlers/finalize "Fill"))))
