(ns repath.studio.elements.views
  (:require [repath.studio.context-menu.views :refer [gen-menu]]
            [clojure.core.matrix :as matrix]
            [reagent.core :as ra]
            [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.mouse :as mouse]
            [reagent.dom :as dom]
            [goog.string :as gstring]))

(defn get-bounds
  "Experimental way of getting the bounds of uknown or complicated elements using the getBBox method.
   SEE https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [element-ref]
  (let [bounds (.getBBox element-ref #js {:stroke true})
        x (.-x bounds)
        y (.-y bounds)]
    [x y (+ x (.-width bounds)) (+ y (.-height bounds))]))

(defn update-bounds
  "We update the bounds on render. As a result, they lag behind the actual element.
   Wrapping the dispatch in requestAnimationFrame helps, but doesn't fix the problem.
   SEE https://developer.mozilla.org/en-US/docs/Web/API/window/requestAnimationFrame"
  [key element-ref]
  (.requestAnimationFrame js/window #(rf/dispatch [:elements/set-property key :bounds (get-bounds (dom/dom-node element-ref)) false])))

(defn render-to-dom
  "We need a reagent form-3 component in order to set the style attribute manually.
   React expects a map, but we need to set a string to avoid serializing css.
   We also experimentally calculate the bounds on updade."
  [{:keys [key attrs type title] :as element} child-elements]
  (ra/create-class
   {:display-name  "element-renderer"

    :componet-did-mount
    (fn
      [this]
      (update-bounds key this)
      (when (not-empty (:style attrs)) (.setAttribute (dom/dom-node this) "style" (:style attrs))))

    :component-did-update
    (fn
      [this _]
      (let [new-argv (rest (ra/argv this))
            style (:style (into {} (:attrs (into {} new-argv))))]
        (update-bounds key this)
        (if (empty? style) (.removeAttribute (dom/dom-node this) "style") (.setAttribute (dom/dom-node this) "style" style))))

    :reagent-render
    (fn
      [{:keys [key attrs type title] :as element} child-elements]
      [type (merge (dissoc attrs :style) {:on-mouse-up     #(mouse/event-handler % element)
                                          :on-mouse-down   #(mouse/event-handler % element)
                                          :on-mouse-move   #(mouse/event-handler % element)})
       (when title [:title title])
       (:content attrs)
       (map (fn [element] ^{:key (:key element)} [tools/render element]) child-elements)])}))

(defmethod tools/render ::tools/element
  [{:keys [children] :as element}]
  (let [child-elements @(rf/subscribe [:elements/filter-visible children])]
    [render-to-dom element child-elements]))

(defn point-of-interest
  [[x y] zoom]
  [:circle {:fill "red"
            :cx x
            :cy y
            :r (/ 3 zoom)}])

(defn handler
  [{:keys [x y key size stroke-width]}]
  (let [element {:key key :type :scale-handler}]
   [:rect {:key key
           :fill "#fff"
           :stroke "#555"
           :stroke-width stroke-width
           :x (- x (/ size 2))
           :y (- y (/ size 2))
           :width size
           :height size
           :on-mouse-up #(mouse/event-handler % element)
           :on-mouse-down #(mouse/event-handler % element)
           :on-mouse-move #(mouse/event-handler % element)}]))

(defn cross
  [{:keys [x y size stroke-width]}]
  [:g
   [:line {:stroke "#555"
           :stroke-width stroke-width
           :x1 (- x (/ size 2))
           :y1 y
           :x2 (+ x (/ size 2))
           :y2 y}]
   [:line {:stroke "#555"
           :stroke-width stroke-width
           :x1 x
           :y1 (- y (/ size 2))
           :x2 x
           :y2 (+ y (/ size 2))}]])

(defn edit
  [bounds zoom]
  (let [[x1 y1 x2 y2] bounds
        [width height] (matrix/sub [x2 y2] [x1 y1])
        handler-size (/ 6 zoom)
        stroke-width (/ 1 zoom)]
    [:g {:key :edit}
     [cross {:x (+ x1 (/ width 2))
             :y (+ y1 (/ height 2))
             :size (/ 10 zoom)
             :stroke-width (/ 1 zoom)}]
     [:text {:x (+ x1 (/ (- x2 x1) 2))
             :y (+ y2 (/ 15 zoom))
             :fill "black"
             :dominant-baseline "middle"
             :text-anchor "middle"
             :font-family "Source Sans Pro"
             :width width
             :font-size (/ 12 zoom)} (-> width (.toFixed 2) (js/parseFloat)) " x " (-> height (.toFixed 2) (js/parseFloat))]
     (map handler [{:x x1 :y y1 :size handler-size :stroke-width stroke-width :key :top-left}
                   {:x x2 :y y1 :size handler-size :stroke-width stroke-width :key :top-right}
                   {:x x1 :y y2 :size handler-size :stroke-width stroke-width :key :bottom-left}
                   {:x x2 :y y2 :size handler-size :stroke-width stroke-width :key :bottom-right}
                   {:x (+ x1 (/ width 2)) :y y1 :size handler-size :stroke-width stroke-width :key :top-middle}
                   {:x x2 :y (+ y1 (/ height 2)) :size handler-size :stroke-width stroke-width :key :right-middle}
                   {:x x1 :y (+ y1 (/ height 2)) :size handler-size :stroke-width stroke-width :key :left-middle}
                   {:x (+ x1 (/ width 2)) :y y2 :size handler-size :stroke-width stroke-width :key :bottom-middle}])]))

(defn bounding-box
  [bounds zoom]
  (let [[x1 y1 x2 y2]    bounds
        stroke-width     (/ 1 zoom)
        stroke-dasharray (/ 5 zoom)
        attrs            {:x x1
                          :y y1
                          :width (- x2 x1)
                          :height (- y2 y1)
                          :stroke-width stroke-width
                          :fill "transparent"}]

    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke "#fff"})]
     [:rect (merge attrs {:stroke "#555" :stroke-dasharray stroke-dasharray})]]))

(defn area
  [area bounds zoom]
  (let [[x1 y1 x2 y2] bounds
        width (- x2 x1)]
    
    (when area
      [:text {:x (+ x1 (/ (- x2 x1) 2))
              :y (+ y1 (/ -15 zoom))
              :fill "black"
              :dominant-baseline "middle"
              :text-anchor "middle"
              :font-family "Source Sans Pro"
              :width width
              :font-size (/ 12 zoom)} (-> area (.toFixed 2) (js/parseFloat)) " px\u00B2"])))

(defn element-menu [e]
  (gen-menu e  [{:name "Cut"
                 :shortcut "Ctrl+X"
                 :action [:elements/cut]}
                {:name "Copy"
                 :shortcut "Ctrl+C"
                 :action [:elements/copy]}
                {:name "Paste"
                 :shortcut "Ctrl+V"
                 :action [:elements/paste]}
                :devider
                {:name "Raise"
                 :shortcut "Page Up"
                 :action [:elements/raise]}
                {:name "Lower"
                 :shortcut "Page Down"
                 :action [:elements/lower]}
                {:name "Raise to top"
                 :shortcut "Home"
                 :action [:elements/raise-to-top]}
                {:name "Lower to bottom"
                 :shortcut "End"
                 :action [:elements/lower-to-bottom]}
                :devider
                {:name "Animate"
                 :action [:elements/animate :animate]}
                {:name "Animate Transform"
                 :action [:elements/animate :animateTransform]}
                {:name "Animate Motion"
                 :action [:elements/animate :animateMotion]}
                :devider
                {:name "Duplicate"
                 :shortcut "Ctrl+D"
                 :action [:elements/duplicate]}
                {:name "Delete"
                 :shortcut "Del"
                 :action [:elements/delete]}]))
