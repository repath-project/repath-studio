(ns renderer.tool.impl.element.core
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.element.circle]
   [renderer.tool.impl.element.ellipse]
   [renderer.tool.impl.element.image]
   [renderer.tool.impl.element.line]
   [renderer.tool.impl.element.polygon]
   [renderer.tool.impl.element.polyline]
   [renderer.tool.impl.element.polyshape]
   [renderer.tool.impl.element.rect]
   [renderer.tool.impl.element.svg]
   [renderer.tool.impl.element.text]
   [renderer.utils.i18n :refer [t]]))

(derive ::tool.hierarchy/element ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/element :idle]
  []
  (t [::help "Click and drag to create an element."]))

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/element
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/snapping-points ::tool.hierarchy/element
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str (name (:tool db)) " edge")})])

(defmethod tool.hierarchy/snapping-elements ::tool.hierarchy/element
  [db]
  (filter :visible (vals (element.handlers/entities db))))
