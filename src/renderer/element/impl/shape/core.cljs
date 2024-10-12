(ns renderer.element.impl.shape.core
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.impl.shape.circle]
   [renderer.element.impl.shape.ellipse]
   [renderer.element.impl.shape.image]
   [renderer.element.impl.shape.line]
   [renderer.element.impl.shape.path]
   [renderer.element.impl.shape.polygon]
   [renderer.element.impl.shape.polyline]
   [renderer.element.impl.shape.polyshape]
   [renderer.element.impl.shape.rect]
   [renderer.element.subs :as-alias element.s]
   [renderer.utils.element :as element]))

(derive ::hierarchy/shape ::hierarchy/graphics)

(defmethod hierarchy/render-to-string ::hierarchy/shape
  [el]
  (let [{:keys [tag attrs title children content]} el
        child-elements @(rf/subscribe [::element.s/filter-visible children])
        attrs (->> (element/style->map attrs)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    [tag
     attrs
     (when title [:title title])
     content
     (doall (map hierarchy/render-to-string child-elements))]))
