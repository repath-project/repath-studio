(ns renderer.utils.overlay
  "Render functions for canvas overlay objects."
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.theme.db :as theme.db]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math]))

(defn point-of-interest
  "Simple dot used for debugging purposes."
  [[x y] & children]
  (let [zoom @(rf/subscribe [::document.s/zoom])]
    (into [:circle {:cx x
                    :cy y
                    :stroke-width 0
                    :fill theme.db/accent
                    :r (/ 3 zoom)}] children)))

(defn line
  ([x1 y1 x2 y2]
   (line x1 y1 x2 y2 true))
  ([x1 y1 x2 y2 dashed?]
   (let [zoom @(rf/subscribe [::document.s/zoom])
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

(defn cross
  ([[x y]]
   (cross x y))
  ([x y]
   (let [zoom @(rf/subscribe [::document.s/zoom])
         size (/ theme.db/handle-size zoom)]
     [:g
      [line (- x (/ size 2)) y (+ x (/ size 2)) y false]
      [line x (- y (/ size 2)) x (+ y (/ size 2)) false]])))

(defn arc
  [[x y] radius start-degrees size-degrees]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        stroke-width (/ 1 zoom)
        radius (/ radius zoom)
        end-degrees (+ start-degrees size-degrees)
        stroke-dasharray (/ theme.db/dash-size zoom)
        x1 (+ x (math/angle-dx start-degrees radius))
        y1 (+ y (math/angle-dy start-degrees radius))
        x2 (+ x (math/angle-dx end-degrees radius))
        y2 (+ y (math/angle-dy end-degrees radius))
        d (str "M" x1 "," y1 " "
               "A" radius "," radius " 0 0,1 " x2 "," y2)
        attrs {:d d
               :fill "transparent"
               :stroke-width stroke-width}]
    [:g
     [:path (merge {:stroke theme.db/accent-inverted} attrs)]
     [:path (merge {:stroke theme.db/accent
                    :stroke-dasharray stroke-dasharray} attrs)]]))

(defn times
  ([[x y]]
   (times x y))
  ([x y]
   (let [zoom @(rf/subscribe [::document.s/zoom])
         size (/ theme.db/handle-size zoom)
         mid (/ size Math/PI)]
     [:g {:style {:pointer-events "none"}}
      [line
       (- x mid) (- y mid)
       (+ x mid) (+ y mid) false]
      [line
       (+ x mid) (- y mid)
       (- x mid) (+ y mid) false]])))

(defn label
  [text position anchor]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        [x y] position
        font-size (/ 10 zoom)
        padding (/ 8 zoom)
        font-width (/ 6 zoom)
        label-width (+ (* (count text) font-width)
                       font-size)
        label-height (+ font-size padding)
        text-anchor (or anchor "middle")]
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
             :font-size font-size} text]]))

(defn size-label
  [bounds]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        [x1 _ x2 y2] bounds
        x (+ x1 (/ (- x2 x1) 2))
        y (+ y2 (/ (+ (/ theme.db/handle-size 2) 15) zoom))
        [width height] (bounds/->dimensions bounds)
        text (str (.toFixed width 2) " x " (.toFixed height 2))]
    [label text [x y]]))

(defn bounding-box
  [bounds dashed?]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        [x1 y1 _x2 _y2] bounds
        [width height] (bounds/->dimensions bounds)
        stroke-width (/ 2 zoom)
        stroke-dasharray (/ theme.db/dash-size zoom)
        attrs {:x x1
               :y y1
               :width width
               :height height
               :shape-rendering "crispEdges"
               :stroke-width stroke-width
               :fill "transparent"}]

    [:g {:style {:pointer-events "none"}}
     [:rect (merge attrs {:stroke theme.db/accent})]
     (when dashed?
       [:rect (merge attrs {:stroke theme.db/accent-inverted
                            :stroke-dasharray stroke-dasharray})])]))

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

(defn centroid
  [el]
  (when-let [pos (element.hierarchy/centroid el)]
    (let [offset (element/offset el)
          pos (mat/add offset pos)]
      [point-of-interest pos
       [:title "Centroid"]])))

(defn area-label
  [area bounds]
  (when area
    (let [zoom @(rf/subscribe [::document.s/zoom])
          [x1 y1 x2 _y2] bounds
          x (+ x1 (/ (- x2 x1) 2))
          y (+ y1 (/ (- -15 (/ theme.db/handle-size 2)) zoom))
          text (str (.toFixed area 2) " px²")]
      [label text [x y]])))

(defn coll->str
  [coll]
  (str "[" (apply str (map #(.toFixed % 2) coll)) "]"))

(defn debug-rows
  []
  [["Dom rect" @(rf/subscribe [::app.s/dom-rect])]
   ["Viewbox" (coll->str @(rf/subscribe [::frame.s/viewbox]))]
   ["Pointer position" (coll->str @(rf/subscribe [::app.s/pointer-pos]))]
   ["Adjusted pointer position" (coll->str @(rf/subscribe [::app.s/adjusted-pointer-pos]))]
   ["Pointer offset" (coll->str @(rf/subscribe [::app.s/pointer-offset]))]
   ["Adjusted pointer offset" (coll->str @(rf/subscribe [::app.s/adjusted-pointer-offset]))]
   ["Pointer drag?" (str @(rf/subscribe [::tool.s/drag]))]
   ["Pan" (coll->str @(rf/subscribe [::document.s/pan]))]
   ["Active tool" @(rf/subscribe [::tool.s/active])]
   ["Primary tool" @(rf/subscribe [::tool.s/primary])]
   ["State"  @(rf/subscribe [::tool.s/state])]
   ["Clicked element" (:id @(rf/subscribe [::app.s/clicked-element]))]
   ["Ignored elements" @(rf/subscribe [::document.s/ignored-ids])]
   ["Snap" (map (fn [[k v]]
                  (str k " - " (if (number? v)
                                 (.toFixed v 2)
                                 (coll->str v)))) @(rf/subscribe [::snap.s/nearest-neighbor]))]])

(defn debug-info
  []
  (into [:div.absolute.top-1.left-2.pointer-events-none
         {:style {:color "#555"}}]
        (for [[s v] (debug-rows)]
          [:div [:strong.mr-1 s] v])))
