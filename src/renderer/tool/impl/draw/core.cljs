(ns renderer.tool.impl.draw.core
  (:require
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.impl.draw.brush]
   [renderer.tool.impl.draw.pen]))

(derive ::tool.hierarchy/draw ::tool.hierarchy/tool)

(defmethod tool.hierarchy/help [::tool.hierarchy/draw :idle]
  []
  "Click and drag to draw.")

(defmethod tool.hierarchy/on-activate ::tool.hierarchy/draw
  [db]
  (-> db
      (history.handlers/reset-state)
      (tool.handlers/set-cursor "crosshair")))
