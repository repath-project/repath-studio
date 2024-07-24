(ns renderer.ruler.views
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [clojure.core.matrix :as mat]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.ruler.subs :as-alias ruler.s]))

(defn bounds
  [orientation size]
  (when-let [bounds @(rf/subscribe [::element.s/bounds])]
    (let [zoom @(rf/subscribe [::document.s/zoom])
          pan @(rf/subscribe [::document.s/pan])
          [x1 y1 x2 y2] (map #(* % zoom) bounds)]
      (if (= orientation :vertical)
        [:rect {:x 0
                :y (- y1 (* (second pan) zoom))
                :width size
                :height (- y2 y1)
                :fill "var(--overlay)"}]
        [:rect {:x (- x1 (* (first pan) zoom))
                :y 0
                :width (- x2 x1)
                :height size
                :fill "var(--overlay)"}]))))

(defn pointer
  [orientation size]
  (let [[x y] @(rf/subscribe [:pointer-pos])
        pointer-size (/ size 5)
        size-diff (- size pointer-size)]
    [:polygon {:points (str/join " " (if (= orientation :vertical)
                                       [size "," y
                                        size-diff "," (- y pointer-size)
                                        size-diff "," (+ y pointer-size)]
                                       [x "," size
                                        (- x pointer-size) "," size-diff
                                        (+ x pointer-size) "," size-diff]))
               :fill "var(--font-color"}]))

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
  [vertical? step font-size text]
  [:text {:x (if vertical? 19 (+ step 4))
          :y (if vertical? (- step 8) (+ font-size 1))
          :writing-mode (when vertical? "vertical-rl")
          :fill "var(--font-color)"
          :font-size font-size
          :rotate (when vertical? 180)
          :font-family "var(--font-mono"}
   (if vertical? (reverse text) text)])

(defn base-lines
  [orientation size]
  (let [[x y] @(rf/subscribe [::frame.s/viewbox])
        zoom @(rf/subscribe [::document.s/zoom])
        steps-coll @(rf/subscribe [::ruler.s/steps-coll orientation])]
    (into [:g]
          (map-indexed
           (fn [i step]
             (let [adjusted-step (* zoom step)
                   font-size 9
                   vertical? (= orientation :vertical)
                   text (-> (+ step (if vertical? y x))
                            Math/round
                            str)]
               (cond
                 (zero? (rem i 10))
                 [:<>
                  [line {:orientation orientation
                         :adjusted-step adjusted-step
                         :size size
                         :starting-point 0}]
                  [label vertical? adjusted-step font-size text]]

                 (and (odd? i) (zero? (rem i 5)))
                 [line {:orientation orientation
                        :adjusted-step adjusted-step
                        :size size
                        :starting-point (/ size 1.6)}]

                 :else
                 [line {:orientation orientation
                        :adjusted-step adjusted-step
                        :size size
                        :starting-point (/ size 1.3)}])))
           steps-coll))))

(defn ruler
  [{:keys [orientation size]}]
  [:svg {:width  (if (= orientation :vertical) size "100%")
         :height (if (= orientation :vertical) "100%" size)}
   [bounds orientation size]
   [base-lines orientation size]
   [pointer orientation size]])

(defn grid-lines
  [orientation]
  (let [zoom @(rf/subscribe [::document.s/zoom])
        [x y width height] @(rf/subscribe [::frame.s/viewbox])
        [width height] (mat/add [width height] [x y])
        steps-coll @(rf/subscribe [::ruler.s/steps-coll orientation])
        vertical? (= orientation :vertical)]
    (into [:g]
          (map-indexed
           (fn [i step]
             (let [step-x (+ step x)
                   step-y (+ step y)
                   main? (zero? (rem i 10))]
               (when (or main? (< zoom 50))
                 [:line {:x1 (if vertical? x step-x)
                         :y1 (if vertical? step-y y)
                         :x2 (if vertical? width step-x)
                         :y2 (if vertical? step-y height)
                         :stroke-width (/ 1 zoom)
                         :opacity (if main? ".3"  ".1")
                         :stroke "#777"
                         :pointer-events "none"}]))) steps-coll))))

(defn grid
  []
  [:<>
   [grid-lines :vertical]
   [grid-lines :horizontal]])
