(ns renderer.tools.element
  (:require
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.tools.base :as tools]
   [renderer.utils.mouse :as mouse]))

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
        element.h/create
        (history.h/finalize (str "Create " (name (:tag temp-element))))
        (assoc :cursor "crosshair"))))

(defn get-bounds
  "Experimental way of getting the bounds of uknown or complicated elements 
   using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [el]
  (let [bounds (.getBBox el)
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
        (doseq [[k v] attrs]
          (.setAttributeNS element nil (name k) v))
        (.appendChild svg element)
        (set! (.-innerHTML element) (if (empty? content) "\u00a0" content))
        (let [bounds (get-bounds element)]
          (.remove element)
          bounds)))))

(defmethod tools/mouse-up :default
  [db e el]
  (if-not (and (= (:button e) 2)
               (:selected? el))
    (-> db
        (dissoc :clicked-element)
        (element.h/select (:key el) (mouse/multiselect? e))
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
        (.setAttribute (.-current ref) "style" (:style attrs)))

      :component-did-update
      (fn
        [this _]
        (let [new-argv (second (ra/argv this))
              style (:style (into {} (:attrs (into {} new-argv))))]
          (.setAttribute (.-current ref) "style" style)))

      :reagent-render
      (fn
        [{:keys [attrs tag title content] :as el}
         child-elements
         default-state?]
        [:<>
         [tag (->> (-> attrs
                       (dissoc :style)
                       (assoc :shape-rendering "geometricPrecision"
                              :ref ref))
                   (remove #(empty? (str (second %))))
                   (into {}))
          (when title [:title title])
          content
          (map (fn [child]
                 ^{:key (:key child)}
                 [tools/render child])
               child-elements)]

         (when default-state?
           (let [mouse-handler #(mouse/event-handler % el)]
             [tag
              (merge (dissoc attrs :style)
                     {:on-pointer-up mouse-handler
                      :on-pointer-down mouse-handler
                      :on-pointer-move mouse-handler
                      :on-double-click mouse-handler
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
