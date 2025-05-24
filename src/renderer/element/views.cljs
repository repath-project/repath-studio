(ns renderer.element.views
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.event.impl.pointer :as event.impl.pointer]))

(def context-menu
  ;; TODO: Add group actions and more.
  [{:label "Cut"
    :action [::element.events/cut]}
   {:label "Copy"
    :action [::element.events/copy]}
   {:label "Paste"
    :action [::element.events/paste]}
   {:type :separator}
   {:label "Raise"
    :action [::element.events/raise]}
   {:label "Lower"
    :action [::element.events/lower]}
   {:label "Raise to top"
    :action [::element.events/raise-to-top]}
   {:label "Lower to bottom"
    :action [::element.events/lower-to-bottom]}
   {:type :separator}
   {:label "Animate"
    :action [::element.events/animate :animate {}]}
   {:label "Animate Transform"
    :action [::element.events/animate :animateTransform {}]}
   {:label "Animate Motion"
    :action [::element.events/animate :animateMotion {}]}
   {:type :separator}
   {:label "Duplicate"
    :action [::element.events/duplicate]}
   {:label "Delete"
    :action [::element.events/delete]}])

(defn ghost-element
  "Renders a ghost element on top of the actual element to ensure that the user
   can interact with it."
  [el]
  (let [{:keys [attrs tag content]} el
        pointer-handler #(event.impl.pointer/handler! % el)
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
  [el _child-els _idle?]
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
        [el child-els idle]
        (let [{:keys [attrs tag title content]} el]
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

           (when idle [ghost-element el])]))})))
