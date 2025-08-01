(ns renderer.ruler.views
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.ruler.subs :as-alias ruler.subs]))

(def ruler-size 24)

(defn bbox-rect
  [orientation]
  (when-let [attrs @(rf/subscribe [::ruler.subs/bbox-rect-attrs orientation ruler-size])]
    [:rect (merge attrs {:fill "var(--overlay)"})]))

(defn pointer
  [orientation]
  (let [[x y] @(rf/subscribe [::app.subs/pointer-pos])
        pointer-size (/ ruler-size 5)
        color "var(--color-accent)"]
    [:g
     [:polygon {:fill color
                :points (string/join " " (if (= orientation :vertical)
                                           [pointer-size "," y
                                            0 "," (- y pointer-size)
                                            0 "," (+ y pointer-size)]
                                           [x "," pointer-size
                                            (- x pointer-size) "," 0
                                            (+ x pointer-size) "," 0]))}]
     [:line (if (= orientation :vertical)
              {:x1 0
               :y1 y
               :x2 ruler-size
               :y2 y
               :stroke color}
              {:x1 x
               :y1 0
               :x2 x
               :y2 ruler-size
               :stroke color})]]))

(defn line
  [{:keys [orientation adjusted-step size starting-point]}]
  [:line (if (= orientation :vertical)
           {:x1 starting-point
            :y1 adjusted-step
            :x2 size
            :y2 adjusted-step
            :stroke "var(--font-color-muted)"}
           {:x1 adjusted-step
            :y1 starting-point
            :x2 adjusted-step
            :y2 size
            :stroke "var(--font-color-muted)"})])

(defn label
  [orientation step font-size text]
  (let [x-step (+ step 4)
        y-step (- step 8)
        vertical (= orientation :vertical)]
    [:text {:x (if vertical 19 x-step)
            :y (if vertical  y-step (inc font-size))
            :writing-mode (when vertical "vertical-rl")
            :fill "var(--font-color)"
            :font-size font-size
            :rotate (when vertical 180)
            :font-family "var(--font-mono)"}
     (if vertical (reverse text) text)]))

(defn base-lines
  [orientation]
  (let [[x y] @(rf/subscribe [::frame.subs/viewbox])
        zoom @(rf/subscribe [::document.subs/zoom])
        steps-coll @(rf/subscribe [::ruler.subs/steps-coll orientation])]
    (into [:g]
          (map-indexed
           (fn [i step]
             (let [adjusted-step (* zoom step)
                   font-size 9
                   text (-> (+ step (if (= orientation :vertical) y x))
                            Math/round
                            str)]
               (cond
                 (zero? (rem i 10))
                 [:<>
                  [line {:orientation orientation
                         :adjusted-step adjusted-step
                         :size ruler-size
                         :starting-point 0}]
                  [label orientation adjusted-step font-size text]]

                 (and (odd? i) (zero? (rem i 5)))
                 [line {:orientation orientation
                        :adjusted-step adjusted-step
                        :size ruler-size
                        :starting-point (/ ruler-size 1.6)}]

                 :else
                 [line {:orientation orientation
                        :adjusted-step adjusted-step
                        :size ruler-size
                        :starting-point (/ ruler-size 1.3)}])))
           steps-coll))))

(defn ruler
  [orientation]
  (let [vertical (= orientation :vertical)]
    [:svg {:width  (if vertical ruler-size "100%")
           :height (if vertical "100%" ruler-size)}
     [bbox-rect orientation]
     [base-lines orientation]
     [pointer orientation]]))

(defn grid-lines
  [orientation]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        [x y w h] @(rf/subscribe [::frame.subs/viewbox])
        [w h] (matrix/add [w h] [x y])
        steps-coll @(rf/subscribe [::ruler.subs/steps-coll orientation])
        vertical (= orientation :vertical)]
    (into [:g]
          (map-indexed
           (fn [i step]
             (let [step-x (+ step x)
                   step-y (+ step y)
                   main? (zero? (rem i 10))]
               (when (or main? (< zoom 50))
                 [:line {:x1 (if vertical x step-x)
                         :y1 (if vertical step-y y)
                         :x2 (if vertical w step-x)
                         :y2 (if vertical step-y h)
                         :stroke-width (/ 1 zoom)
                         :opacity (when-not main? ".5")
                         :stroke "var(--border-color)"
                         :pointer-events "none"}]))) steps-coll))))

(defn grid
  []
  [:<>
   [grid-lines :vertical]
   [grid-lines :horizontal]])
