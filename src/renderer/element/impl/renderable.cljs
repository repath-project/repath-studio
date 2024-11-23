(ns renderer.element.impl.renderable
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.dom :as dom]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]))

(derive ::hierarchy/renderable ::hierarchy/element)

(defmethod hierarchy/bounds ::hierarchy/renderable
  [{:keys [tag attrs content] :as el}]
  (when-let [svg (dom/canvas-element!)]
    (let [dom-el (js/document.createElementNS "http://www.w3.org/2000/svg" (name tag))]
      (doseq [[k v] attrs]
        (when (element/supported-attr? (dissoc el :attrs) k)
          (.setAttributeNS dom-el nil (name k) v)))
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [bounds (bounds/dom-el->bounds dom-el)]
        (.remove dom-el)
        bounds))))

(defn ghost-element
  "Renders a ghost element on top of the actual element to ensure that the user
   can interact with it."
  [el]
  (let [{:keys [attrs tag content]} el
        pointer-handler #(pointer/event-handler! % el)
        zoom @(rf/subscribe [::document.s/zoom])
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
    (ra/create-class
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
        (let [new-argv (second (ra/argv this))
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
            ^{:key (:id child)} [hierarchy/render child])]

         (when default-state? [ghost-element el])])})))

(defmethod hierarchy/render ::hierarchy/renderable
  [el]
  (let [child-els @(rf/subscribe [::element.s/filter-visible (:children el)])
        state @(rf/subscribe [::tool.s/state])]
    [render-to-dom el child-els (= state :idle)]))
