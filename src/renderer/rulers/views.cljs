(ns renderer.rulers.views
  (:require
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn bounds
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
                :fill "var(--level-3)"}]))))

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

(defn label
  [vertical? step font-size text]
  [:text {:x (if vertical? 19 (+ step 4))
          :y (if vertical? (- step 10) (+ font-size 1))
          :writing-mode (when vertical? "vertical-rl")
          :fill "var(--font-color)"
          :font-size font-size
          :rotate (when vertical? 180)
          :font-family "var(--font-mono"}
   (if vertical? (reverse text) text)])

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
   [mouse-pointer orientation size]])

(defn grid
  [position step]
  [:div.absolute.inset-0.pointer-events-none
   {:style {:background-position position
            :background-size (str step  "px " step "px")
            :background-image (str "linear-gradient(to right, #77777715 1px, transparent 1px), 
                                    linear-gradient(to bottom, #77777715 1px, transparent 1px)")}}])

(defn grids
  []
  (let [zoom @(rf/subscribe [:document/zoom])
        position @(rf/subscribe [:rulers/position])
        step @(rf/subscribe [:rulers/step])
        step (* zoom step)]
    [:<>
     [grid position step]
     [grid position (* step 10)]]))
