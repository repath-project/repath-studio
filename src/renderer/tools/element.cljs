(ns renderer.tools.element
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.dom :as dom]
   [renderer.utils.pointer :as pointer]))

(derive ::tools/element ::tools/tool)

(defmethod tools/activate ::tools/element
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (h/set-message [:div "Click and drag to create an element."])))

(defmethod tools/drag-start ::tools/element
  [db]
  (h/set-state db :create))

(defmethod tools/drag-end ::tools/element
  [db]
  (let [temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (-> db
        element.h/add
        (history.h/finalize "Create " (name (:tag temp-element)))
        (assoc :cursor "crosshair"))))

(defmethod tools/bounds ::tools/element
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

(defmethod tools/pointer-up :default
  [db e el]
  (if-not (and (= (:button e) :right)
               (:selected? el))
    (-> db
        (dissoc :clicked-element)
        (element.h/select (:key el) (pointer/multiselect? e))
        (history.h/finalize "Select element"))
    (dissoc db :clicked-element)))

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
            ^{:key (:key child)} [tools/render child])]

         (when default-state?
           (let [pointer-handler #(pointer/event-handler % el)]
             [tag
              (merge (dissoc attrs :style)
                     {:on-pointer-up pointer-handler
                      :on-pointer-down pointer-handler
                      :on-pointer-move pointer-handler
                      :on-double-click pointer-handler
                      :shape-rendering "optimizeSpeed"
                      :fill "transparent"
                      :stroke "transparent"
                      :stroke-width (max (:stroke-width attrs)
                                         (/ 20 @(rf/subscribe [:document/zoom])))})
              content]))])})))

(defmethod tools/render ::tools/element
  [{:keys [children] :as el}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        state @(rf/subscribe [:state])
        zoom @(rf/subscribe [:document/zoom])]
    [render-to-dom el child-elements (= state :default) zoom]))
