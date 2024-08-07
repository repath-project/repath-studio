(ns renderer.tool.animation.core
  "https://svgwg.org/specs/animations/#AnimationElements"
  (:require
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.tool.animation.animate-motion]
   [renderer.tool.animation.animate-transform]
   [renderer.tool.animation.animate]))

(derive ::tool/animation ::tool/descriptive)

(defmethod tool/render ::tool/animation
  [{:keys [children tag attrs]}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [tag
     attrs
     (for [el child-elements]
       ^{:key (:key el)} [tool/render el])]))
