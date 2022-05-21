(ns repath.studio.tools.group
  (:require [reagent.core :as ra]
            [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.mouse :as mouse]
            [repath.studio.units :as units]))

(derive :g ::tools/container)

(defmethod tools/properties :g [] {:description "The <g> SVG element is a container used to group other SVG elements."})

(defmethod tools/render :g
  [{:keys [key attrs children pierce?] :as element}]
  (let [elements @(rf/subscribe [:elements])
        child-elements @(rf/subscribe [:elements/filter-visible children])
        [x1 y1 x2 y2] (tools/bounds element elements)]
    [:<>
     [:g attrs
      (map (fn [child] ^{:key (:key child)} [tools/render child]) child-elements)]
     (when-not pierce? [:rect {:x x1
                               :y y1
                               :width (- x2 x1)
                               :height (- y2 y1)
                               :fill "transparent"
                               :on-mouse-up   #(mouse/event-handler % element)
                               :on-mouse-down #(mouse/event-handler % element)
                               :on-mouse-move #(mouse/event-handler % element)
                               :on-double-click #(rf/dispatch [:elements/set-property key :pierce? true false])}])]))


(defmethod tools/translate :g
  [db element [x y]] element)
