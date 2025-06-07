(ns renderer.element.impl.renderable
  "https://www.w3.org/TR/SVG/render.html#TermRenderableElement"
  (:require
   [re-frame.core :as rf]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]))

(derive ::element.hierarchy/renderable ::element.hierarchy/element)

(defmethod element.hierarchy/bbox ::element.hierarchy/renderable
  [{:keys [content] :as el}]
  (when-let [svg (utils.dom/canvas-element!)]
    (let [dom-el (utils.element/->dom-element el)]
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [bbox (utils.bounds/dom-el->bbox dom-el)]
        (.remove dom-el)
        bbox))))

(defmethod element.hierarchy/render ::element.hierarchy/renderable
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        state @(rf/subscribe [::tool.subs/state])]
    [element.views/render-to-dom el child-els (= state :idle)]))
