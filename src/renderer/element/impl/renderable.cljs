(ns renderer.element.impl.renderable
  "https://www.w3.org/TR/SVG/render.html#TermRenderableElement"
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.element.subs :as-alias element.subs]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.element :as utils.element]
   [renderer.utils.pointer :as utils.pointer]))

(derive ::element.hierarchy/renderable ::element.hierarchy/element)

(defmethod element.hierarchy/bbox ::element.hierarchy/renderable
  [{:keys [tag attrs content] :as el}]
  (when-let [svg (utils.dom/canvas-element!)]
    (let [dom-el (js/document.createElementNS "http://www.w3.org/2000/svg" (name tag))]
      (doseq [[k v] attrs]
        (when (utils.element/supported-attr? (dissoc el :attrs) k)
          (.setAttributeNS dom-el nil (name k) v)))
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [bbox (utils.bounds/dom-el->bbox dom-el)]
        (.remove dom-el)
        bbox))))

(defn ghost-element
  "Renders a ghost element on top of the actual element to ensure that the user
   can interact with it."
  [el]
  (let [{:keys [attrs tag content]} el
        pointer-handler #(utils.pointer/event-handler! % el)
        zoom @(rf/subscribe [::document.subs/zoom])
        stroke-width (max (:stroke-width attrs) (/ 20 zoom))]
    [tag
     (merge (dissoc attrs :style)
            {:on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler
             :shape-rendering "optimizeSpeed"
             :fill "transparent"
             :stroke "transparent"
             :stroke-width stroke-width})
     content]))

(defn render-to-dom
  "We need a reagent form-3 component in order to set the style attribute manually.
   React expects a map, but we need to set a string to avoid serializing css."
  [el]
  (let [ref (react/createRef)]
    (reagent/create-class
     {:display-name "element-renderer"

      :component-did-mount
      (fn
        [_this]
        (when (.-pauseAnimations (.-current ref))
          (.pauseAnimations (.-current ref)))
        (.setAttribute (.-current ref) "style" (-> el :attrs :style)))

      :component-did-update
      (fn
        [this _]
        (let [new-argv (second (reagent/argv this))
              style (:style (into {} (:attrs (into {} new-argv))))]
          (.setAttribute (.-current ref) "style" style)))

      :reagent-render
      (fn
        [{:keys [attrs tag title content] :as el} child-els default-state?]
        [:<>
         [tag (->> (-> attrs
                       (dissoc :style)
                       (assoc :shape-rendering "geometricPrecision"
                              :ref ref))
                   (remove #(empty? (str (second %))))
                   (into {}))
          (when title [:title title])
          content
          (for [child child-els]
            ^{:key (:id child)} [element.hierarchy/render child])]

         (when default-state? [ghost-element el])])})))

(defmethod element.hierarchy/render ::element.hierarchy/renderable
  [el]
  (let [child-els @(rf/subscribe [::element.subs/filter-visible (:children el)])
        state @(rf/subscribe [::tool.subs/state])]
    [render-to-dom el child-els (= state :idle)]))
