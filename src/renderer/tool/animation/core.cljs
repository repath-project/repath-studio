(ns renderer.tool.animation.core
  "https://svgwg.org/specs/animations/#AnimationElements"
  (:require
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.animation.animate]
   [renderer.tool.animation.animate-motion]
   [renderer.tool.animation.animate-transform]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive ::tool.hierarchy/animation ::tool.hierarchy/descriptive)

(defmethod tool.hierarchy/render ::tool.hierarchy/animation
  [{:keys [children tag attrs id]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [tag
     attrs
     (for [el child-elements]
       ^{:key id} [tool.hierarchy/render el])]))
