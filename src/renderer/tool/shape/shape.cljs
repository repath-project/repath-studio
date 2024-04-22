(ns renderer.tool.shape.shape
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tool.base :as tool]))

(derive ::tool/shape ::tool/graphics)

(defmethod tool/render-to-string ::tool/shape
  [{:keys [tag attrs title children content]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        attrs (->> (dissoc attrs :style)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         content
         (doall (map tool/render-to-string child-elements))])))
