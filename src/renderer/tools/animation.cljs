(ns renderer.tools.animation
  "https://svgwg.org/specs/animations/#AnimationElements"
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]))

(derive ::tools/animation ::tools/descriptive)

(defmethod tools/render ::tools/animation
  [{:keys [children tag attrs]}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])]
    [tag
     attrs
     (for [el child-elements]
       ^{:key (:key el)} [tools/render el])]))

(defmethod tools/bounds ::tools/animation [] nil)
