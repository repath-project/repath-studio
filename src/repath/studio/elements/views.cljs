(ns repath.studio.elements.views
  (:require [repath.studio.context-menu.views :refer [gen-menu]]
            [re-frame.core :as rf]
            [repath.studio.mouse :as mouse]
            [repath.studio.bounds :as bounds]
            [repath.studio.styles :as styles]))

(defn point-of-interest
  [[x y]]
  (let [zoom @(rf/subscribe [:zoom])]
    [:circle {:fill "red"
              :cx x
              :cy y
              :r (/ 3 zoom)}]))

(defn square-handler
  [{:keys [x y key] :as element}]
  (let [zoom @(rf/subscribe [:zoom])
        size (/ 8 zoom)
        stroke-width (/ 1 zoom)]
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
  [bounds]
  (let [zoom @(rf/subscribe [:zoom])
        [x1 y1 x2 y2] bounds
        [width height] (bounds/->dimensions bounds)]
    [:g {:key :bounding-handlers}
     [cross {:x (+ x1 (/ width 2))
             :y (+ y1 (/ height 2))
             :size (/ 10 zoom)
             :stroke-width (/ 1 zoom)}]
     (map (fn [handler] [square-handler handler]) [{:x x1 :y y1 :key :top-left :type :handler :tag :scale}
                                                   {:x x2 :y y1 :key :top-right :type :handler :tag :scale}
                                                   {:x x1 :y y2 :key :bottom-left :type :handler :tag :scale}
                                                   {:x x2 :y y2 :key :bottom-right :type :handler :tag :scale}
                                                   {:x (+ x1 (/ width 2)) :y y1 :key :top-middle :type :handler :tag :scale}
                                                   {:x x2 :y (+ y1 (/ height 2)) :key :middle-right :type :handler :tag :scale}
                                                   {:x x1 :y (+ y1 (/ height 2)) :key :middle-left :type :handler :tag :scale}
                                                   {:x (+ x1 (/ width 2)) :y y2 :key :bottom-middle :type :handler :tag :scale}])]))

(defn label
  [text position]
  (let [zoom @(rf/subscribe [:zoom])
        [x y] position
        font-size (/ 12 zoom)
        padding 8
        font-width 5
        label-width (/ (+ (* (count text) font-width) 12) zoom)
        label-height (/ (+ 12 padding) zoom)]
    [:g
     [:rect {:x (- x (/ label-width 2))
             :y (- y  (/ label-height 2))
             :fill "rgba(0, 0, 0, .7)"
             :font-family "Source Sans Pro"
             :rx (/ 4 zoom)
             :width label-width
             :height label-height} text]
     [:text {:x x
             :y y
             :fill "white"
             :dominant-baseline "middle"
             :text-anchor "middle"
             :font-family "Source Sans Pro"
             :font-weight "bold"
             :width label-width
             :font-size font-size} text]]))

(defn size
  [bounds]
  (let [zoom @(rf/subscribe [:zoom])
        [x1 _ x2 y2] bounds
        x (+ x1 (/ (- x2 x1) 2))
        y (+ y2 (/ 20 zoom))
        [width height] (bounds/->dimensions bounds)
        text (str (-> width (.toFixed 2) (js/parseFloat)) " x " (-> height (.toFixed 2) (js/parseFloat)))]
    [label text [x y]]))

(defn bounding-box
  [bounds]
  (let [zoom             @(rf/subscribe [:zoom])
        [x1 y1 _ _]      bounds
        [width height]   (bounds/->dimensions bounds)
        stroke-width     (/ 1 zoom)
        stroke-dasharray (/ 5 zoom)
        attrs            {:x x1
                          :y y1
                          :width width
                          :height height
                          :stroke-width stroke-width
                          :fill "transparent"}]

    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke "#fff"})]
     [:rect (merge attrs {:stroke "#555" :stroke-dasharray stroke-dasharray})]]))

(defn select-box
  [adjusted-mouse-pos adjusted-mouse-offset zoom]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos]
    {:tag :rect :attrs {:key    :select
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
  [area bounds]
  (when area
    (let [zoom @(rf/subscribe [:zoom])
          [x1 y1 x2 _] bounds
          x (+ x1 (/ (- x2 x1) 2))
          y (+ y1 (/ -20 zoom))
          text (str (-> area (.toFixed 2) (js/parseFloat)) " px\u00B2")]
      [label text [x y] zoom])))

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
                                           :divider
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
                                           :divider
                                           {:name "Animate"
                                            :action [:elements/animate :animate {}]}
                                           {:name "Animate Transform"
                                            :action [:elements/animate :animateTransform {}]}
                                           {:name "Animate Motion"
                                            :action [:elements/animate :animateMotion {}]}
                                           :divider
                                           {:name "Duplicate in position"
                                            :shortcut "Ctrl+D"
                                            :action [:elements/duplicate-in-posistion]}
                                           {:name "Delete"
                                            :shortcut "Del"
                                            :action [:elements/delete]}]))))
