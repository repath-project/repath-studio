(ns repath.studio.elements.views
  (:require [repath.studio.context-menu.views :refer [gen-menu]]
            [clojure.core.matrix :as matrix]
            [re-frame.core :as rf]
            [repath.studio.mouse :as mouse]))

(defn point-of-interest
  [[x y] zoom]
  [:circle {:fill "red"
            :cx x
            :cy y
            :r (/ 3 zoom)}])

(defn handler
  [{:keys [x y key size stroke-width cursor]}]
  (let [element {:key key :type :scale-handler :cursor cursor}]
   [:rect {:key key
           :fill "#fff"
           :stroke "#555"
           :stroke-width stroke-width
           :x (- x (/ size 2))
           :y (- y (/ size 2))
           :width size
           :height size
           :cursor cursor
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

(defn bounding-handlers
  [bounds zoom]
  (let [[x1 y1 x2 y2] bounds
        [width height] (matrix/sub [x2 y2] [x1 y1])
        handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)]
    [:g {:key :bounding-handlers}
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
     (map handler [{:x x1 :y y1 :size handler-size :stroke-width stroke-width :key :top-left :cursor :nwse-resize}
                   {:x x2 :y y1 :size handler-size :stroke-width stroke-width :key :top-right :cursor :nesw-resize}
                   {:x x1 :y y2 :size handler-size :stroke-width stroke-width :key :bottom-left :cursor :nesw-resize}
                   {:x x2 :y y2 :size handler-size :stroke-width stroke-width :key :bottom-right :cursor :nwse-resize}
                   {:x (+ x1 (/ width 2)) :y y1 :size handler-size :stroke-width stroke-width :key :top-middle :cursor :ns-resize}
                   {:x x2 :y (+ y1 (/ height 2)) :size handler-size :stroke-width stroke-width :key :middle-right :cursor :ew-resize}
                   {:x x1 :y (+ y1 (/ height 2)) :size handler-size :stroke-width stroke-width :key :middle-left :cursor :ew-resize}
                   {:x (+ x1 (/ width 2)) :y y2 :size handler-size :stroke-width stroke-width :key :bottom-middle :cursor :ns-resize}])]))

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
  (let [state @(rf/subscribe [:state])]
    (when (= state :default) (gen-menu e  [{:name "Cut"
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
                                            :action [:elements/animate :animate {}]}
                                           {:name "Animate Transform"
                                            :action [:elements/animate :animateTransform {}]}
                                           {:name "Animate Motion"
                                            :action [:elements/animate :animateMotion {}]}
                                           :devider
                                           {:name "Duplicate"
                                            :shortcut "Ctrl+D"
                                            :action [:elements/duplicate]}
                                           {:name "Delete"
                                            :shortcut "Del"
                                            :action [:elements/delete]}]))))
