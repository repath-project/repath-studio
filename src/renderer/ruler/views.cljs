(ns renderer.ruler.views
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.ruler.db :refer [Orientation]]
   [renderer.ruler.subs :as-alias ruler.subs]))

(m/=> bbox-rect [:-> Orientation any?])
(defn bbox-rect
  [orientation]
  (when-let [attrs @(rf/subscribe [::ruler.subs/bbox-rect-attrs orientation])]
    [:rect (merge attrs {:fill "var(--overlay)"})]))

(m/=> pointer [:-> Orientation any?])
(defn pointer
  [orientation]
  (let [[x y] @(rf/subscribe [::app.subs/pointer-pos])
        ruler-size @(rf/subscribe [::ruler.subs/size])
        pointer-size (/ ruler-size 5)
        size-diff (- ruler-size pointer-size)]
    [:polygon {:points (string/join " " (if (= orientation :vertical)
                                          [ruler-size "," y
                                           size-diff "," (- y pointer-size)
                                           size-diff "," (+ y pointer-size)]
                                          [x "," ruler-size
                                           (- x pointer-size) "," size-diff
                                           (+ x pointer-size) "," size-diff]))
               :fill "var(--font-color"}]))

(m/=> line [:-> map? any?])
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

(m/=> label [:-> Orientation number? number? string? any?])
(defn label
  [orientation step font-size text]
  (let [vertical (= orientation :vertical)]
    [:text {:x (if vertical 19 (+ step 4))
            :y (if vertical (- step 8) (+ font-size 1))
            :writing-mode (when vertical "vertical-rl")
            :fill "var(--font-color)"
            :font-size font-size
            :rotate (when vertical 180)
            :font-family "var(--font-mono"}
     (if vertical (reverse text) text)]))

(m/=> base-lines [:-> Orientation any?])
(defn base-lines
  [orientation]
  (let [[x y] @(rf/subscribe [::frame.subs/viewbox])
        zoom @(rf/subscribe [::document.subs/zoom])
        steps-coll @(rf/subscribe [::ruler.subs/steps-coll orientation])
        ruler-size @(rf/subscribe [::ruler.subs/size])]
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

(m/=> ruler [:-> Orientation any?])
(defn ruler
  [orientation]
  (let [ruler-size @(rf/subscribe [::ruler.subs/size])
        vertical (= orientation :vertical)]
    [:svg {:width  (if vertical ruler-size "100%")
           :height (if vertical "100%" ruler-size)}
     [bbox-rect orientation]
     [base-lines orientation]
     [pointer orientation]]))

(m/=> grid-lines [:-> Orientation any?])
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
                         :opacity (if main? ".3"  ".1")
                         :stroke "#777"
                         :pointer-events "none"}]))) steps-coll))))

(defn grid
  []
  [:<>
   [grid-lines :vertical]
   [grid-lines :horizontal]])
