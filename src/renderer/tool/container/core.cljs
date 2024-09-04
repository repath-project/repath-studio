(ns renderer.tool.container.core
  "https://www.w3.org/TR/SVG/struct.html#TermContainerElement"
  (:require
   ["style-to-object" :default parse]
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.container.canvas]
   [renderer.tool.container.group]
   [renderer.tool.container.svg]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive ::tool.hierarchy/container ::tool.hierarchy/box)

(derive :a ::tool.hierarchy/container)
(derive :clipPath ::tool.hierarchy/container)
(derive :defs ::tool.hierarchy/container)
(derive :marker ::tool.hierarchy/container)
(derive :mask ::tool.hierarchy/container)
(derive :pattern ::tool.hierarchy/container)
(derive :switch ::tool.hierarchy/container)
(derive :symbol ::tool.hierarchy/container)

(defmethod tool.hierarchy/render ::tool.hierarchy/container
  [{:keys [children tag attrs id]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [tag attrs (for [el child-elements]
                 ^{:key id} [tool.hierarchy/render el])]))

(defmethod tool.hierarchy/render-to-string ::tool.hierarchy/container
  [{:keys [tag attrs title children]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])
        attrs (->> (update attrs :style parse)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         (map tool.hierarchy/render-to-string child-elements)])))
