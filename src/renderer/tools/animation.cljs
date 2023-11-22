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
     (map (fn [element]
            ^{:key (:key element)}
            [tools/render element])
          child-elements)]))
