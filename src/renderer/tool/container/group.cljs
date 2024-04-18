(ns renderer.tool.container.group
  "https://www.w3.org/TR/SVG/struct.html#GElement"
  (:require
   [re-frame.core :as rf]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]))

(derive :g ::tool/container)

(defmethod tool/properties :g
  []
  {:description "The <g> SVG element is a container used to group other 
                 SVG elements."
   :attrs [:transform]})

(defmethod tool/translate :g
  [el [_x _y]]
  el) ; TODO

(defmethod tool/render :g
  [{:keys [attrs children bounds] :as element}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        ignored-keys @(rf/subscribe [:document/ignored-keys])
        ignored? (contains? ignored-keys (:key element))
        [x1 y1 _x2 _y2] bounds
        [width height] (bounds/->dimensions bounds)
        pointer-handler #(pointer/event-handler % element)]
    [:g attrs
     (map (fn [element] [tool/render element]) (merge child-elements))
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

(defmethod tool/bounds :g
  [el elements]
  (let [children (vals (select-keys elements (:children el)))]
    (element/bounds children)))
