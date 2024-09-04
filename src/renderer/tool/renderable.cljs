(ns renderer.tool.renderable
  (:require
   ["react" :as react]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.app.handlers :as app.h]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.dom :as dom]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]))

(defmethod tool.hierarchy/activate ::tool.hierarchy/renderable
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (app.h/set-message "Click and drag to create an element.")))

(defmethod tool.hierarchy/drag-start ::tool.hierarchy/renderable
  [db]
  (app.h/set-state db :create))

(defmethod tool.hierarchy/drag-end ::tool.hierarchy/renderable
  [db _e]
  (-> db
      (element.h/add)
      (app.h/set-tool :select)
      (app.h/set-state :default)
      (app.h/explain "Create " (name (:tag (element.h/get-temp db))))))

(defmethod tool.hierarchy/bounds ::tool.hierarchy/renderable
  [{:keys [tag attrs content] :as el}]
  (when-let [svg (dom/canvas-element)]
    (let [dom-el (js/document.createElementNS "http://www.w3.org/2000/svg" (name tag))]
      (doseq [[k v] attrs]
        (when (element/supported-attr? (dissoc el :attrs) k)
          (.setAttributeNS dom-el nil (name k) v)))
      (.appendChild svg dom-el)
      (set! (.-innerHTML dom-el) (if (empty? content) "\u00a0" content))
      (let [bounds (bounds/from-bbox dom-el)]
        (.remove dom-el)
        bounds))))

(defmethod tool.hierarchy/position ::tool.hierarchy/renderable
  [el position]
  (let [center (bounds/center (tool.hierarchy/bounds el))
        offset (mat/sub position center)]
    (tool.hierarchy/translate el offset)))

(defn ghost-element
  "Renders a ghost element on top of the actual element to ensure that the user
   can interact with it."
  [{:keys [attrs tag content] :as el}]
  (let [pointer-handler #(pointer/event-handler % el)
        zoom @(rf/subscribe [::document.s/zoom])
        stroke-width (max (:stroke-width attrs) (/ 20 zoom))]
    [tag
     (merge (dissoc attrs :style)
            {:on-pointer-up pointer-handler
             :on-pointer-down pointer-handler
             :on-pointer-move pointer-handler
             :on-double-click pointer-handler
             :shape-rendering "optimizeSpeed"
             :fill "transparent"
             :stroke "transparent"
             :stroke-width stroke-width})
     content]))

(defn render-to-dom
  "We need a reagent form-3 component in order to set the style attribute manually.
   React expects a map, but we need to set a string to avoid serializing css."
  [{:keys [attrs]}]
  (let [ref (react/createRef)]
    (ra/create-class
     {:display-name "element-renderer"

      :component-did-mount
      (fn
        [_this]
        (when (.-pauseAnimations (.-current ref))
          (.pauseAnimations (.-current ref)))
        (.setAttribute (.-current ref) "style" (:style attrs)))

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
            ^{:key (:id child)} [tool.hierarchy/render child])]

         (when default-state? [ghost-element el])])})))

(defmethod tool.hierarchy/render ::tool.hierarchy/renderable
  [{:keys [children] :as el}]
  (let [child-els @(rf/subscribe [::element.s/filter-visible children])
        state @(rf/subscribe [::app.s/state])]
    [render-to-dom el child-els (= state :default)]))
