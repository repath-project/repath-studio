(ns renderer.tools.shape
  "https://www.w3.org/TR/SVG/shapes.html#TermShapeElement"
  (:require
   ["element-to-path" :as element-to-path]
   [goog.string :as g.str]
   [re-frame.core :as rf]
   [reagent.dom.server :as server]
   [renderer.tools.base :as tools]))

(derive ::tools/shape ::tools/graphics)

(defmethod tools/path ::tools/shape
  [{:keys [attrs tag]}]
  (element-to-path (clj->js {:name tag
                             :attributes attrs})))

(defmethod tools/render-to-string ::tools/renderable
  [{:keys [tag attrs title children content]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        attrs (->> (dissoc attrs :style)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         content
         (map tools/render-to-string child-elements)]
        (server/render-to-static-markup)
        (g.str/unescapeEntities))))
