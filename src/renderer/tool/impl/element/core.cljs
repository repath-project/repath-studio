(ns renderer.tool.impl.element.core
  (:require
   [renderer.tool.handlers :as h]
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
      (h/set-cursor "crosshair")
      (dissoc :drag :pointer-offset :clicked-element)
      (h/dissoc-temp)))

(defmethod hierarchy/drag-start ::hierarchy/element
  [db]
  (h/set-state db :create))

(defmethod hierarchy/drag-end ::hierarchy/element
  [db _e]
  (-> db
      (h/create-temp-element)
      (h/activate :transform)
      (h/set-state :idle)
      (h/explain (str "Create " (name (:tag (h/temp db)))))))

(defmethod hierarchy/snapping-bases ::hierarchy/element
  [db]
  [(:adjusted-pointer-pos db)])
