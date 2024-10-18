(ns renderer.tool.impl.element.core
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.tool.impl.element.circle]
   [renderer.tool.impl.element.ellipse]
   [renderer.tool.impl.element.image]
   [renderer.tool.impl.element.line]
   [renderer.tool.impl.element.polygon]
   [renderer.tool.impl.element.polyline]
   [renderer.tool.impl.element.polyshape]
   [renderer.tool.impl.element.rect]
   [renderer.tool.impl.element.svg]
   [renderer.tool.impl.element.text]))

(derive ::hierarchy/element ::hierarchy/tool)

(defmethod hierarchy/help [::hierarchy/element :idle]
  []
  "Click and drag to create an element.")

(defmethod hierarchy/activate ::hierarchy/element
  [db]
  (-> db
      (app.h/set-cursor "crosshair")
      (dissoc :drag :pointer-offset :clicked-element)
      (element.h/dissoc-temp)))

(defmethod hierarchy/drag-start ::hierarchy/element
  [db]
  (app.h/set-state db :create))

(defmethod hierarchy/drag-end ::hierarchy/element
  [db _e]
  (-> db
      (element.h/add)
      (app.h/set-tool :transform)
      (app.h/set-state :idle)
      (app.h/explain "Create " (name (:tag (element.h/temp db))))))
