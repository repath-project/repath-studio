(ns renderer.tools.group
  "https://www.w3.org/TR/SVG/struct.html#GElement"
  (:require
   [renderer.tools.base :as tools]
   [re-frame.core :as rf]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.mouse :as mouse]))

(derive :g ::tools/container)

(defmethod tools/properties :g
  []
  {:description "The <g> SVG element is a container used to group other 
                 SVG elements."})

(defmethod tools/translate :g
  [element [_x _y]]
  element)

(defmethod tools/render :g
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])
        elements @(rf/subscribe [:document/elements])
        ignored-keys @(rf/subscribe [:document/ignored-keys])
        ignored? (contains? ignored-keys (:key element))
        bounds (tools/bounds element elements)
        [x1 y1 _x2 _y2] bounds
        [width height] (bounds/->dimensions bounds)]
    [:g attrs
     (map (fn [element] [tools/render element]) (merge child-elements))
     [:rect {:x x1
             :y y1
             :width width
             :height height
             :fill "transparent"
             :pointer-events (when ignored? "none")
             :on-double-click #(mouse/event-handler % element)
             :on-pointer-up #(mouse/event-handler % element)
             :on-pointer-down #(mouse/event-handler % element)
             :on-pointer-move #(mouse/event-handler % element)}]]))