(ns renderer.tools.element
  (:require
   [renderer.tools.base :as tools]
   [renderer.elements.handlers :as element-handlers]
   [reagent.core :as ra]
   [re-frame.core :as rf]
   [renderer.history.handlers :as history]
   [renderer.handlers :as handlers]
   [renderer.utils.mouse :as mouse]
   [reagent.dom :as dom]))

(derive ::tools/element ::tools/tool)

(defmethod tools/activate ::tools/element
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (handlers/set-message [:div "Click and drag to create an element."])))

(defmethod tools/drag-start ::tools/element
  [db]
  (handlers/set-state db :create))

(defmethod tools/drag-end ::tools/element
  [db]
  (let [temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (-> db
        (element-handlers/create-from-temp)
        (history/finalize (str "Create " (name (:tag temp-element))))
        (assoc :cursor "crosshair"))))

(defn get-bounds
  "Experimental way of getting the bounds of uknown or complicated elements 
   using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [element]
  (let [bounds (.getBBox element)
        x1 (.-x bounds)
        y1 (.-y bounds)
        x2 (+ x1 (.-width bounds))
        y2 (+ y1 (.-height bounds))]
    [x1 y1 x2 y2]))

(defmethod tools/bounds ::tools/element
  [{:keys [tag attrs content]}]
  (when-let [frame (.getElementById js/document "frame")]
    (when-let [svg (.getElementById (.. frame -contentWindow -document) "canvas")]
      (let [element (js/document.createElementNS "http://www.w3.org/2000/svg" (name tag))]
        (doseq [[key value] attrs]
          (.setAttributeNS element nil (name key) value))
        (.appendChild svg element)
        (set! (.-innerHTML element) (if (empty? content) "\u00a0" content))
        (let [bounds (get-bounds element)]
          (.remove element)
          bounds)))))

(defmethod tools/mouse-up :default
  [db event element]
  (if-not (and (= (:button event) 2)
               (:selected? element))
    (-> db
        (dissoc :clicked-element)
        (element-handlers/select (mouse/multiselect? event) element)
        (history/finalize "Select element"))
    (dissoc db :clicked-element)))

(defn render-to-dom
  "We need a reagent form-3 component in order to set the style attribute manually.
   React expects a map, but we need to set a string to avoid serializing css."
  [{:keys [attrs]}]
  (ra/create-class
   {:display-name "element-renderer"

    :component-did-mount
    (fn
      [this]
      (.setAttribute (dom/dom-node this) "style" (:style attrs)))

    :component-did-update
    (fn
      [this _]
      (let [new-argv (second (ra/argv this))
            style (:style (into {} (:attrs (into {} new-argv))))]
        (.setAttribute (dom/dom-node this) "style" style)))

    :reagent-render
    (fn
      [{:keys [attrs tag title content] :as element}
       child-elements
       default-state?]
      [:<>
       [tag (->> (-> attrs
                     (dissoc :style)
                     (assoc :shape-rendering "geometricPrecision"))
                 (remove #(empty? (str (second %))))
                 (into {}))
        (when title [:title title])
        content
        (map (fn [child]
               ^{:key (:key child)}
               [tools/render child])
             child-elements)]

       (when default-state?
         [tag
          (merge (dissoc attrs :style)
                 {:on-pointer-up #(mouse/event-handler % element)
                  :on-pointer-down #(mouse/event-handler % element)
                  :on-pointer-move #(mouse/event-handler % element)
                  :on-double-click #(mouse/event-handler % element)
                  :shape-rendering "optimizeSpeed"
                  :fill "transparent"
                  :stroke "transparent"
                  :stroke-width (max (:stroke-width attrs)
                                     (/ 20 @(rf/subscribe [:document/zoom])))})
          content])])}))

(defmethod tools/render ::tools/element
  [{:keys [children] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])
        state @(rf/subscribe [:state])
        zoom @(rf/subscribe [:document/zoom])]
    [render-to-dom element child-elements (= state :default) zoom]))
