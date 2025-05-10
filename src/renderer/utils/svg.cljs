(ns renderer.utils.svg
  "Render functions for canvas overlay objects."
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.document.subs :as-alias document.subs]
   [renderer.theme.db :as theme.db]
   [renderer.utils.bounds :as utils.bounds :refer [BBox]]
   [renderer.utils.hiccup :refer [Hiccup]]
   [renderer.utils.math :as utils.math :refer [Vec2]]))

(m/=> dot [:-> Vec2 Hiccup any?])
(defn dot
  [[x y] & children]
  (let [zoom @(rf/subscribe [::document.subs/zoom])]
    (into [:circle {:cx x
                    :cy y
                    :stroke-width 0
                    :fill theme.db/accent
                    :r (/ 3 zoom)}] children)))

(m/=> line [:function
            [:-> Vec2 Vec2 any?]
            [:-> Vec2 Vec2 boolean? any?]])
(defn line
  ([[x1 y1] [x2 y2]]
   [line [x1 y1] [x2 y2] true])
  ([[x1 y1] [x2 y2] dashed?]
   (let [zoom @(rf/subscribe [::document.subs/zoom])
         stroke-width (/ 1 zoom)
         stroke-dasharray (/ 5 zoom)
         attrs {:x1 x1
                :y1 y1
                :x2 x2
                :y2 y2
                :stroke-width stroke-width
                :shape-rendering (when dashed? "crispEdges")}]
     [:g
      (when dashed? [:line (merge attrs {:stroke theme.db/accent-inverted})])
      [:line (merge attrs
                    {:stroke theme.db/accent
                     :stroke-dasharray (when dashed? stroke-dasharray)})]])))

(m/=> cross [:-> Vec2 any?])
(defn cross
  [[x y]]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        size (/ theme.db/handle-size zoom)]
    [:g
     [line
      [(- x (/ size 2)) y]
      [(+ x (/ size 2)) y]
      false]
     [line
      [x (- y (/ size 2))]
      [x (+ y (/ size 2))]
      false]]))

(m/=> arc [:-> Vec2 number? number? number? any?])
(defn arc
  [[x y] radius start-degrees size-degrees]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        stroke-width (/ 1 zoom)
        radius (/ radius zoom)
        end-degrees (+ start-degrees size-degrees)
        stroke-dasharray (/ theme.db/dash-size zoom)
        x1 (+ x (utils.math/angle-dx start-degrees radius))
        y1 (+ y (utils.math/angle-dy start-degrees radius))
        x2 (+ x (utils.math/angle-dx end-degrees radius))
        y2 (+ y (utils.math/angle-dy end-degrees radius))
        d (str "M" x1 "," y1 " "
               "A" radius "," radius " 0 0,1 " x2 "," y2)
        attrs {:d d
               :fill "transparent"
               :stroke-width stroke-width}]
    [:g
     [:path (merge {:stroke theme.db/accent-inverted} attrs)]
     [:path (merge {:stroke theme.db/accent
                    :stroke-dasharray stroke-dasharray} attrs)]]))

(m/=> times [:-> Vec2 any?])
(defn times
  [[x y]]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        size (/ theme.db/handle-size zoom)
        mid (/ size Math/PI)]
    [:g {:style {:pointer-events "none"}}
     [line
      [(- x mid) (- y mid)]
      [(+ x mid) (+ y mid)]
      false]
     [line
      [(+ x mid) (- y mid)]
      [(- x mid) (+ y mid)]
      false]]))

(m/=> label [:function
             [:-> string? Vec2 any?]
             [:-> string? Vec2 [:enum "start" "middle" "end"] any?]])
(defn label
  ([text position]
   [label text position "middle"])
  ([text position text-anchor]
   (let [zoom @(rf/subscribe [::document.subs/zoom])
         [x y] position
         font-size (/ 10 zoom)
         padding (/ 8 zoom)
         font-width (/ 6 zoom)
         label-width (+ (* (count text) font-width)
                        font-size)
         label-height (+ font-size padding)]
     [:g
      [:rect {:x (case text-anchor
                   "start" (- x (/ padding 2))
                   "middle" (- x (/ label-width 2))
                   "end" (- x label-width (/ (- padding) 2)))
              :y (- y  (/ label-height 2))
              :fill theme.db/accent
              :rx (/ 4 zoom)
              :width label-width
              :height label-height} text]
      [:text {:x x
              :y y
              :fill theme.db/accent-inverted
              :dominant-baseline "middle"
              :text-anchor text-anchor
              :width label-width
              :font-family theme.db/font-mono
              :font-size font-size} text]])))

(m/=> bounding-box [:-> BBox boolean? any?])
(defn bounding-box
  [bbox dashed?]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [min-x min-y] bbox
        [w h] (utils.bounds/->dimensions bbox)
        stroke-width (/ 1 zoom)
        stroke-dasharray (/ theme.db/dash-size zoom)
        attrs {:x min-x
               :y min-y
               :width w
               :height h
               :shape-rendering "crispEdges"
               :stroke-width stroke-width
               :fill "transparent"}]

    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke theme.db/accent})]
     (when dashed?
       [:rect (merge attrs {:stroke theme.db/accent-inverted
                            :stroke-dasharray stroke-dasharray})])]))

(m/=> select-box [:-> App any?])
(defn select-box
  [db]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        [pos-x pos-y] (:adjusted-pointer-pos db)
        [offset-x offset-y] (:adjusted-pointer-offset db)]
    {:tag :rect
     :attrs {:x (min pos-x offset-x)
             :y (min pos-y offset-y)
             :width (abs (- pos-x offset-x))
             :height (abs (- pos-y offset-y))
             :shape-rendering "crispEdges"
             :fill-opacity ".1"
             :fill theme.db/accent
             :stroke theme.db/accent
             :stroke-opacity ".5"
             :stroke-width (/ 1 zoom)}}))
