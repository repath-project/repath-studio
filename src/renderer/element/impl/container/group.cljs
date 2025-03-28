(ns renderer.element.impl.container.group
  "https://www.w3.org/TR/SVG/struct.html#GElement
   https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Element/g"
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]))

(derive :g ::hierarchy/container)

(defmethod hierarchy/properties :g
  []
  {:icon "group"
   :label "Group"
   :description "The <g> SVG element is a container used to group other
                 SVG elements."
   :attrs [:transform]})

(defn translate!
  [transform [x y]]
  (let [g (js/document.createElementNS "http://www.w3.org/2000/svg" "g")
        _ (.setAttributeNS g nil "transform" (or transform ""))
        m (.consolidate (.. g -transform -baseVal))
        matrix (if m (.-matrix m) (js/DOMMatrixReadOnly.))
        matrix (.translate matrix x y)]
    (.toString matrix)))

(defmethod hierarchy/translate :g
  [el offset]
  (update-in el [:attrs :transform] translate! offset))

(defmethod hierarchy/render :g
  [el]
  (let [{:keys [attrs children bbox]} el
        child-els @(rf/subscribe [::element.s/filter-visible children])]
    [:g (element/style->map attrs)
     (for [child child-els]
       ^{:key (:id child)} [hierarchy/render child])
     (when bbox
       (let [ignored-ids @(rf/subscribe [::document.s/ignored-ids])
             ignored? (contains? ignored-ids (:id el))
             [min-x min-y] bbox
             [w h] (bounds/->dimensions bbox)
             pointer-handler #(pointer/event-handler! % el)
             zoom @(rf/subscribe [::document.s/zoom])
             stroke-width (max (:stroke-width attrs) (/ 20 zoom))]
         [:rect {:x min-x
                 :y min-y
                 :width w
                 :height h
                 :fill "transparent"
                 :stroke "transparent"
                 :stroke-width stroke-width
                 :pointer-events (when ignored? "none")
                 :on-pointer-up pointer-handler
                 :on-pointer-down pointer-handler
                 :on-pointer-move pointer-handler}]))]))
