(ns renderer.tool.shape.core
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.shape.circle]
   [renderer.tool.shape.ellipse]
   [renderer.tool.shape.image]
   [renderer.tool.shape.line]
   [renderer.tool.shape.path]
   [renderer.tool.shape.polygon]
   [renderer.tool.shape.polyline]
   [renderer.tool.shape.polyshape]
   [renderer.tool.shape.rect]
   [renderer.utils.element :as element]))

(derive ::tool.hierarchy/shape ::tool.hierarchy/graphics)

(defmethod tool.hierarchy/render-to-string ::tool.hierarchy/shape
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
     (doall (map tool.hierarchy/render-to-string child-elements))]))
