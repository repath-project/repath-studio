(ns renderer.tool.impl.element.core
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.snap.handlers :as snap.h]
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
  (-> (h/set-cursor db "crosshair")
      (snap.h/update-tree)))

(defmethod hierarchy/drag-start ::hierarchy/element
  [db]
  (h/set-state db :create))

(defmethod hierarchy/drag-end ::hierarchy/element
  [db _e]
  (-> db
      (h/create-temp-element)
      (h/activate :transform)
      (h/explain (str "Create " (name (:tag (h/temp db)))))))

(defmethod hierarchy/snapping-bases ::hierarchy/element
  [db]
  [(:adjusted-pointer-pos db)])

(defmethod hierarchy/snapping-points ::hierarchy/element
  [db]
  (let [visible-elements (filter :visible (vals (element.h/entities db)))]
    (element.h/snapping-points db visible-elements)))
