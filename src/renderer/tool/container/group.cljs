(ns renderer.tool.container.group
  "https://www.w3.org/TR/SVG/struct.html#GElement"
  (:require
   ["style-to-object" :default parse]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.pointer :as pointer]))

(derive :g ::tool/container)

(defmethod tool/properties :g
  []
  {:description "The <g> SVG element is a container used to group other
                 SVG elements."
   :attrs [:transform]})

(defn- translate
  [transform [x y]]
  (let [g (js/document.createElementNS "http://www.w3.org/2000/svg" "g")
        _ (.setAttributeNS g nil "transform" (or transform ""))
        m (.consolidate (.. g -transform -baseVal))
        matrix (if m (.-matrix m) (js/DOMMatrixReadOnly.))
        matrix (.translate matrix x y)]
    (.toString matrix)))

(defmethod tool/translate :g
  [el _offset]
  el
  #_(update-in el [:attrs :transform] translate offset))

(defmethod tool/render :g
  [{:keys [attrs children bounds] :as el}]
  (let [child-els @(rf/subscribe [::element.s/filter-visible children])]
    [:g (update attrs :style parse)
     (for [child child-els]
       ^{:key (name (:id child))} [tool/render child])
     (when bounds
       (let [ignored-ids @(rf/subscribe [::document.s/ignored-ids])
             ignored? (contains? ignored-ids (:id el))
             [x1 y1 _x2 _y2] bounds
             [width height] (bounds/->dimensions bounds)
             pointer-handler #(pointer/event-handler % el)
             zoom @(rf/subscribe [::document.s/zoom])
             stroke-width (max (:stroke-width attrs) (/ 20 zoom))]
         [:rect {:x x1
                 :y y1
                 :width width
                 :height height
                 :fill "transparent"
                 :stroke "transparent"
                 :stroke-width stroke-width
                 :pointer-events (when ignored? "none")
                 :on-double-click pointer-handler
                 :on-pointer-up pointer-handler
                 :on-pointer-down pointer-handler
                 :on-pointer-move pointer-handler}]))]))

