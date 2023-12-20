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
     (map (fn [el] ^{:key (:key el)} [tools/render el]) child-elements)]))

(defmethod tools/bounds ::tools/animation [] nil)
