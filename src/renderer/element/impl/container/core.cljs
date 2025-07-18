(ns renderer.element.impl.container.core
  "https://www.w3.org/TR/SVG/struct.html#TermContainerElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.impl.container.canvas]
   [renderer.element.impl.container.group]
   [renderer.element.impl.container.svg]
   [renderer.element.subs :as-alias element.subs]
   [renderer.utils.element :as utils.element]))

(derive ::element.hierarchy/container ::element.hierarchy/box)

(derive :a ::element.hierarchy/container)
(derive :clipPath ::element.hierarchy/container)
(derive :defs ::element.hierarchy/container)
(derive :marker ::element.hierarchy/container)
(derive :mask ::element.hierarchy/container)
(derive :pattern ::element.hierarchy/container)
(derive :switch ::element.hierarchy/container)
(derive :symbol ::element.hierarchy/container)

(defmethod element.hierarchy/render ::element.hierarchy/container
  [el]
  (let [{:keys [children tag attrs id]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])]
    [tag attrs (for [el child-elements]
                 ^{:key id}
                 [element.hierarchy/render el])]))

(defmethod element.hierarchy/render-to-string ::element.hierarchy/container
  [el]
  (let [{:keys [tag attrs title children]} el
        child-elements @(rf/subscribe [::element.subs/filter-visible children])
        attrs (->> (utils.element/style->map attrs)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    (into [tag attrs (when title [:title title])]
          (map element.hierarchy/render-to-string child-elements))))
