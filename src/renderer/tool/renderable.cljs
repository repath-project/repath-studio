(ns renderer.tool.renderable
  (:require
   ["react" :as react]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.dom :as dom]
   [renderer.utils.pointer :as pointer]))

(defmethod tool/activate ::tool/renderable
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (h/set-message [:div "Click and drag to create an element."])))

(defmethod tool/drag-start ::tool/renderable
  [db]
  (h/set-state db :create))

(defmethod tool/drag-end ::tool/renderable
  [db]
  (let [temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (-> db
        element.h/add
        (h/set-tool :select)
        (h/set-state :default)
        (history.h/finalize "Create " (name (:tag temp-element))))))

(defmethod tool/bounds ::tool/renderable
  [{:keys [tag attrs content]}]
  (when-let [svg (dom/canvas-element)]
    (let [el (js/document.createElementNS "http://www.w3.org/2000/svg" (name tag))]
      (doseq [[k v] attrs]
        (.setAttributeNS el nil (name k) v))
      (.appendChild svg el)
      (set! (.-innerHTML el) (if (empty? content) "\u00a0" content))
      (let [bounds (bounds/from-bbox el)]
        (.remove el)
        bounds))))

(defmethod tool/position ::tool/renderable
  [el position]
  (let [center (bounds/center (tool/bounds el))
        offset (mat/sub position center)]
    (tool/translate el offset)))

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
        [{:keys [attrs tag title content] :as el} child-elements default-state?]
        [:<>
         [tag (->> (-> attrs
                       (dissoc :style)
                       (assoc :shape-rendering "geometricPrecision"
                              :ref ref))
                   (remove #(empty? (str (second %))))
                   (into {}))
          (when title [:title title])
          content
          (for [child child-elements]
            ^{:key (:key child)} [tool/render child])]

         (when default-state? [ghost-element el])])})))

(defmethod tool/render ::tool/renderable
  [{:keys [children] :as el}]
  (let [child-elements @(rf/subscribe [::element.s/filter-visible children])
        state @(rf/subscribe [:state])]
    [render-to-dom el child-elements (= state :default)]))
