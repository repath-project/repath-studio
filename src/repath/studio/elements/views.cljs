(ns repath.studio.elements.views
  (:require [repath.studio.context-menu.views :refer [gen-menu]]
            [clojure.core.matrix :as matrix]
            [re-frame.core :as rf]
            [repath.studio.mouse :as mouse]
            [repath.studio.styles :as styles]))

(defn point-of-interest
  [[x y] zoom]
  [:circle {:fill "red"
            :cx x
            :cy y
            :r (/ 3 zoom)}])

(defn scale-handler
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
           :cursor "default"
           :on-mouse-up #(mouse/event-handler % element)
           :on-mouse-down #(mouse/event-handler % element)
           :on-mouse-move #(mouse/event-handler % element)}]))

(defn edit-handler
  [{:keys [x y key size stroke-width]}]
  (let [element {:key key :type :edit-handler}]
    [:rect {:key key
            :fill "#fff"
            :stroke "#555"
            :stroke-width stroke-width
            :x (- x (/ size 2))
            :y (- y (/ size 2))
            :width size
            :height size
            :cursor "default"
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
           :y2 y
           :pointer-events "none"}]
   [:line {:stroke "#555"
           :stroke-width stroke-width
           :x1 x
           :y1 (- y (/ size 2))
           :x2 x
           :y2 (+ y (/ size 2))
           :pointer-events "none"}]])

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
     (map scale-handler [{:x x1 :y y1 :size handler-size :stroke-width stroke-width :key :top-left}
                   {:x x2 :y y1 :size handler-size :stroke-width stroke-width :key :top-right}
                   {:x x1 :y y2 :size handler-size :stroke-width stroke-width :key :bottom-left}
                   {:x x2 :y y2 :size handler-size :stroke-width stroke-width :key :bottom-right}
                   {:x (+ x1 (/ width 2)) :y y1 :size handler-size :stroke-width stroke-width :key :top-middle}
                   {:x x2 :y (+ y1 (/ height 2)) :size handler-size :stroke-width stroke-width :key :middle-right}
                   {:x x1 :y (+ y1 (/ height 2)) :size handler-size :stroke-width stroke-width :key :middle-left}
                   {:x (+ x1 (/ width 2)) :y y2 :size handler-size :stroke-width stroke-width :key :bottom-middle}])]))

(defn size
  [bounds zoom]
  (let [[x1 y1 x2 y2] bounds
        [width height] (matrix/sub [x2 y2] [x1 y1])]
     [:text {:key :size
             :x (+ x1 (/ (- x2 x1) 2))
             :y (+ y2 (/ 15 zoom))
             :fill "black"
             :dominant-baseline "middle"
             :text-anchor "middle"
             :font-family "Source Sans Pro"
             :width width
             :font-size (/ 12 zoom)} (-> width (.toFixed 2) (js/parseFloat)) " x " (-> height (.toFixed 2) (js/parseFloat))]))

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

(defn select-box
  [adjusted-mouse-pos adjusted-mouse-offset zoom]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos]
    {:type :rect :attrs {:key    :select
                         :x      (min pos-x offset-x)
                         :y      (min pos-y offset-y)
                         :width  (Math/abs (- pos-x offset-x))
                         :height (Math/abs (- pos-y offset-y))
                         :fill   styles/accent
                         :fill-opacity ".25"
                         :stroke styles/accent
                         :stroke-opacity ".5"
                         :stroke-width (/ 1 zoom)}}))

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
                                           {:name "Duplicate in position"
                                            :shortcut "Ctrl+D"
                                            :action [:elements/duplicate-in-posistion]}
                                           {:name "Delete"
                                            :shortcut "Del"
                                            :action [:elements/delete]}]))))
