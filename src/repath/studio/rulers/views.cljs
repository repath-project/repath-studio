(ns repath.studio.rulers.views
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [repath.studio.styles :as styles]))

(defn selected-bounds
  [orientation size]
  (let [bounds @(rf/subscribe [:elements/bounds])
        zoom @(rf/subscribe [:zoom])
        pan @(rf/subscribe [:pan])
        [x1 y1 x2 y2] (map #(* % zoom) bounds)]
    (if (= orientation :vertical) 
      [:rect {:x 0 :y (- y1 (* (second pan) zoom)) :width size :height (- y2 y1) :fill styles/level-3}]
      [:rect {:x (- x1 (* (first pan) zoom)) :y 0 :width (- x2 x1) :height size :fill styles/level-3}])))

(defn mouse-pointer
  [orientation size]
  (let [[x y] @(rf/subscribe [:mouse-pos])
        pointer-size (/ size 5)]
    [:polygon {:points (str/join " " (if (= orientation :vertical)
                                       [size "," y (- size pointer-size) "," (- y pointer-size) (- size pointer-size) "," (+ y pointer-size)]
                                       [x "," size (- x pointer-size) "," (- size pointer-size) (+ x pointer-size) "," (- size pointer-size)]))
               :fill styles/font-color}]))

(defn line
  [{:keys [orientation adjusted-step size starting-point]}]
  [:line (if (= orientation :vertical)
           {:x1 starting-point :y1 adjusted-step :x2 size :y2 adjusted-step :stroke styles/font-color-muted}
           {:x1 adjusted-step :y1 starting-point :x2 adjusted-step :y2 size :stroke styles/font-color-muted})])

(defn base-lines
  [orientation size]
  (let [[x y]      @(rf/subscribe [:canvas/viewbox])
        zoom       @(rf/subscribe [:zoom])
        steps-coll @(rf/subscribe [:rullers/steps-coll orientation])]
    (into [:g]
          (map-indexed
           (fn [i step]
             (let [adjusted-step (* zoom step)
                   font-size 9]
               (cond
                 (zero? (rem i 10)) [:<>
                                     [line {:orientation orientation
                                            :adjusted-step adjusted-step
                                            :size size
                                            :starting-point 0}]
                                     [:text {:x (if (= orientation :vertical) 6 (+ adjusted-step 4))
                                             :y (if (= orientation :vertical) (+ adjusted-step 4) (+ font-size 1))
                                             :writing-mode (when (= orientation :vertical) "vertical-rl")
                                             :style (when (= orientation :vertical) {:text-orientation "upright"})
                                             :fill styles/font-color
                                             :font-size font-size
                                             :font-family styles/font-family}
                                      (Math/round (if (= orientation :vertical) (+ step y) (+ step x)))]]
                 (and (odd? i) (zero? (rem i 5))) [line {:orientation orientation
                                                         :adjusted-step adjusted-step
                                                         :size size
                                                         :starting-point (/ size 1.6)}]
                 :else [line {:orientation orientation
                              :adjusted-step adjusted-step
                              :size size
                              :starting-point (/ size 1.3)}])))
           steps-coll))))

(defn ruler
  [{:keys [orientation size]}]
  [:svg {:width  (if (= orientation :vertical) size "100%")
         :height (if (= orientation :vertical) "100%" size)
         :style {:box-sizing "border-box"}}
   [selected-bounds orientation size]
   [base-lines orientation size]
   [mouse-pointer orientation size]])

(defn grid-lines
  [orientation]
  (let [[x y width height] @(rf/subscribe [:canvas/viewbox])
        zoom               @(rf/subscribe [:zoom])
        steps-coll         @(rf/subscribe [:rullers/steps-coll orientation])
        vertical?          (= orientation :vertical)]
    (into [:g]
          (map-indexed
           (fn [i step]
             (when (zero? (rem i 10))
               [:line {:x1 (if vertical? x (+ step x))
                       :y1 (if vertical? (+ step y) y)
                       :x2 (if vertical? width (+ step x))
                       :y2 (if vertical? (+ step y) height)
                       :stroke-width (/ 1 zoom)
                       :opacity ".4"
                       :stroke styles/level-3}]))) steps-coll)))

(defn grid
  []
  [:<>
   [grid-lines :vertical]
   [grid-lines :horizontal]])
