(ns renderer.tool.impl.draw.core
  (:require
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.tool.impl.draw.brush]
   [renderer.tool.impl.draw.pen]))

(derive ::hierarchy/draw ::hierarchy/tool)

(defmethod hierarchy/help [::hierarchy/draw :idle]
  []
  "Click and drag to draw.")

(defmethod hierarchy/activate ::hierarchy/draw
  [db]
  (h/set-cursor db "crosshair"))
