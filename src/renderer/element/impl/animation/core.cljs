(ns renderer.element.impl.animation.core
  "https://svgwg.org/specs/animations/#AnimationElements"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.impl.animation.animate]
   [renderer.element.impl.animation.animate-motion]
   [renderer.element.impl.animation.animate-transform]
   [renderer.element.subs :as-alias element.s]))

(derive ::hierarchy/animation ::hierarchy/descriptive)

(defmethod hierarchy/render ::hierarchy/animation
  [el]
  (let [{:keys [children tag attrs id]} el
        child-elements @(rf/subscribe [::element.s/filter-visible children])]
    [tag attrs (for [el child-elements]
                 ^{:key id} [hierarchy/render el])]))
