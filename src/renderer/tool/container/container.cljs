(ns renderer.tool.container.container
  "https://www.w3.org/TR/SVG/struct.html#TermContainerElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tool.base :as tool]))

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
  (let [child-elements @(rf/subscribe [:element/filter-visible children])]
    [tag attrs (for [el child-elements]
                 ^{:key (:key el)} [tool/render el])]))

(defmethod tool/render-to-string ::tool/container
  [{:keys [tag attrs title children]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        attrs (->> (dissoc attrs :style)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (-> [tag
         attrs
         (when title [:title title])
         (map tool/render-to-string child-elements)])))
