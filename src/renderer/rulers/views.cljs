(ns renderer.rulers.views
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]
   [clojure.core.matrix :as mat]))

(defn selected-bounds
  [orientation size]
  (when-let [bounds @(rf/subscribe [:element/bounds])]
    (let [zoom @(rf/subscribe [:document/zoom])
          pan @(rf/subscribe [:document/pan])
          [x1 y1 x2 y2] (map #(* % zoom) bounds)]
      (if (= orientation :vertical)
        [:rect {:x 0
                :y (- y1 (* (second pan) zoom))
                :width size
                :height (- y2 y1)
                :fill "var(--level-3)"}]
        [:rect {:x (- x1 (* (first pan) zoom))
                :y 0
                :width (- x2 x1)
                :height size
                :fill "var(--level-3)"}]))
    ;; Alternative view when page bounds are visible
    #_(let [position (- size 6)
            zoom @(rf/subscribe [:document/zoom])
            [x y] @(rf/subscribe [:document/pan])
            [x1 y1 x2 y2] (map #(* % zoom) bounds)]
        (if (= orientation :vertical)
          [:<>
           [:rect {:x position
                   :y (- y1 (* y zoom))
                   :width 1
                   :height (- y2 y1)
                   :fill "var(--font-color)"}]
           [:circle {:cx position
                     :cy (- y1 (* y zoom))
                     :r 3
                     :fill "var(--level-3)"
                     :stroke "var(--font-color)"}]
           [:circle {:cx position
                     :cy (- y2 (* y zoom))
                     :fill "var(--level-3)"
                     :r 3
                     :stroke "var(--font-color)"}]]
          [:<>
           [:rect {:x (- x1 (* x zoom))
                   :y position
                   :width (- x2 x1)
                   :height 1
                   :fill "var(--font-color)"}]
           [:circle {:cx (- x1 (* x zoom))
                     :cy position
                     :fill "var(--level-3)"
                     :r 3
                     :stroke "var(--font-color)"}]
           [:circle {:cx (- x2 (* x zoom))
                     :cy position
                     :fill "var(--level-3)"
                     :r 3
                     :stroke "var(--font-color)"}]]))))

#_(defn page-bounds
    [orientation]
    (let [{:keys [attrs]} @(rf/subscribe [:element/active-page])
          {:keys [x y width height]} attrs
          zoom @(rf/subscribe [:document/zoom])
          [pan-x pan-y] @(rf/subscribe [:document/pan])
          [x y] (mat/sub [x y] [pan-x pan-y])
          [x y width height] (map #(* % zoom) [x y width height])]
      (if (= orientation :vertical)
        [:rect {:x 0 :y y :width 23 :height height :fill "var(--level-3)"}]
        [:rect {:x x :y 0 :width width :height 23 :fill "var(--level-3)"}])))

(defn mouse-pointer
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

(defn base-lines
  [orientation size]
  (let [[x y] @(rf/subscribe [:frame/viewbox])
        zoom @(rf/subscribe [:document/zoom])
        steps-coll @(rf/subscribe [:rulers/steps-coll orientation])]
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
                  [:text {:x (if vertical? 19 (+ adjusted-step 4))
                          :y (if vertical? (- adjusted-step 10) (+ font-size 1))
                          :writing-mode (when vertical? "vertical-rl")
                          :fill "var(--font-color)"
                          :font-size font-size
                          :rotate (when vertical? 180)
                          :font-family "var(--font-mono"}
                   (if vertical? (reverse text) text)]]

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
   [selected-bounds orientation size]
   [base-lines orientation size]
   [mouse-pointer orientation size]])

(defn grid-lines
  [orientation]
  (let [zoom @(rf/subscribe [:document/zoom])
        [x y width height] @(rf/subscribe [:frame/viewbox])
        [width height] (mat/add [width height] [x y])
        steps-coll @(rf/subscribe [:rulers/steps-coll orientation])
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
