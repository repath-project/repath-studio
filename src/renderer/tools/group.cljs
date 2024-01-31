(ns renderer.tools.group
  "https://www.w3.org/TR/SVG/struct.html#GElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.pointer :as pointer]))

(derive :g ::tools/container)

(defmethod tools/properties :g
  []
  {:description "The <g> SVG element is a container used to group other 
                 SVG elements."
   :attrs [:transform]})

(defmethod tools/translate :g
  [el [_x _y]]
  el) ; TODO

(defmethod tools/render :g
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        elements @(rf/subscribe [:document/elements])
        ignored-keys @(rf/subscribe [:document/ignored-keys])
        ignored? (contains? ignored-keys (:key element))
        bounds (tools/bounds element elements)
        [x1 y1 _x2 _y2] bounds
        [width height] (bounds/->dimensions bounds)
        pointer-handler #(pointer/event-handler % element)]
    [:g attrs
     (map (fn [element] [tools/render element]) (merge child-elements))
     [:rect {:x x1
             :y y1
             :width width
             :height height
             :fill "transparent"
             :pointer-events (when ignored? "none")
             :on-double-click pointer-handler
             :on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler}]]))

(defmethod tools/bounds :g
  [el elements]
  (let [children (vals (select-keys elements (:children el)))]
    (tools/elements-bounds elements children)))