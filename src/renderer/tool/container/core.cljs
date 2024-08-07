(ns renderer.tool.container.core
  "https://www.w3.org/TR/SVG/struct.html#TermContainerElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.tool.container.canvas]
   [renderer.tool.container.group]
   [renderer.tool.container.svg]))

(derive ::tool/container ::tool/box)

(derive :a ::tool/container)
(derive :clipPath ::tool/container)
(derive :defs ::tool/container)
(derive :marker ::tool/container)
(derive :mask ::tool/container)
(derive :pattern ::tool/container)
(derive :switch ::tool/container)
(derive :symbol ::tool/container)

(defmethod tool/render ::tool/container
  [{:keys [children tag attrs]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [tag attrs (for [el child-elements]
                 ^{:key (:key el)} [tool/render el])]))

(defmethod tool/render-to-string ::tool/container
  [{:keys [tag attrs title children]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])
        attrs (->> (dissoc attrs :style)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         (map tool/render-to-string child-elements)])))
