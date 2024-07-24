(ns renderer.tool.shape.core
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.tool.shape.circle]
   [renderer.tool.shape.ellipse]
   [renderer.tool.shape.image]
   [renderer.tool.shape.line]
   [renderer.tool.shape.path]
   [renderer.tool.shape.polygon]
   [renderer.tool.shape.polyline]
   [renderer.tool.shape.polyshape]
   [renderer.tool.shape.rect]))

(derive ::tool/shape ::tool/graphics)

(defmethod tool/render-to-string ::tool/shape
  [{:keys [tag attrs title children content]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])
        attrs (->> (dissoc attrs :style)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         content
         (doall (map tool/render-to-string child-elements))])))
