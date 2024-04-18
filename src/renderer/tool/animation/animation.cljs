(ns renderer.tool.animation.animation
  "https://svgwg.org/specs/animations/#AnimationElements"
  (:require
   [re-frame.core :as rf]
   [renderer.tool.base :as tool]))

(derive ::tool/animation ::tool/descriptive)

(defmethod tool/render ::tool/animation
  [{:keys [children tag attrs]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])]
    [tag
     attrs
     (for [el child-elements]
       ^{:key (:key el)} [tool/render el])]))
