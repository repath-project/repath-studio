(ns renderer.tool.shape.shape
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   ["element-to-path" :as element-to-path]
   [re-frame.core :as rf]
   [renderer.tool.base :as tool]))

(derive ::tool/shape ::tool/graphics)

(defmethod tool/path ::tool/shape
  [{:keys [attrs tag]}]
  (element-to-path (clj->js {:name tag
                             :attributes attrs})))

(defmethod tool/render-to-string ::tool/renderable
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
