(ns renderer.element.impl.container.core
  "https://www.w3.org/TR/SVG/struct.html#TermContainerElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.impl.container.canvas]
   [renderer.element.impl.container.group]
   [renderer.element.impl.container.svg]
   [renderer.element.subs :as-alias element.s]
   [renderer.utils.element :as element]))

(derive ::hierarchy/container ::hierarchy/box)

(derive :a ::hierarchy/container)
(derive :clipPath ::hierarchy/container)
(derive :defs ::hierarchy/container)
(derive :marker ::hierarchy/container)
(derive :mask ::hierarchy/container)
(derive :pattern ::hierarchy/container)
(derive :switch ::hierarchy/container)
(derive :symbol ::hierarchy/container)

(defmethod hierarchy/render ::hierarchy/container
  [el]
  (let [{:keys [children tag attrs id]} el
        child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [tag attrs (for [el child-elements] ^{:key id} [hierarchy/render el])]))

(defmethod hierarchy/render-to-string ::hierarchy/container
  [el]
  (let [{:keys [tag attrs title children]} el
        child-elements @(rf/subscribe [::element.s/filter-visible children])
        attrs (->> (element/style->map attrs)
                   (remove #(empty? (str (second %))))
                   (into {}))]
    [tag attrs (when title [:title title]) (map hierarchy/render-to-string child-elements)]))
