(ns renderer.tool.impl.element.core
  (:require
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history.h]
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

(defmethod hierarchy/on-activate ::hierarchy/element
  [db]
  (h/set-cursor db "crosshair"))

(defmethod hierarchy/on-drag-start ::hierarchy/element
  [db]
  (h/set-state db :create))

(defmethod hierarchy/on-drag-end ::hierarchy/element
  [db _e]
  (-> db
      (h/create-temp-element)
      (h/activate :transform)
      (history.h/finalize (str "Create " (name (:tag (h/temp db)))))))

(defmethod hierarchy/snapping-points ::hierarchy/element
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str (name (:tool db)) " edge")})])

(defmethod hierarchy/snapping-elements ::hierarchy/element
  [db]
  (filter :visible (vals (element.h/entities db))))
