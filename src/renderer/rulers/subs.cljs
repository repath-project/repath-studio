(ns renderer.rulers.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :rulers/step
 :<- [:document/zoom]
 (fn [zoom _]
   ;; Any attemt to ingeniously produce this mapping was proven inferior.
   ;; Simply returning a fixed step depending on the zoom range works fine.
   ;; Zoom levels outside of this range are considered invalid for now.
   ;; Maybe we need to revisit this at some point.
   (condp > zoom
     0.001 2000
     0.025 1000
     0.05 500
     0.1 200
     0.25 100
     0.5 50
     1 15
     2 10
     5 5
     10 2
     25 1
     50 0.5
     0.1)))

(rf/reg-sub
 :rullers/steps-coll
 :<- [:rulers/step]
 :<- [:frame/viewbox]
 (fn [[ruler-step [x y width height]] [_ orientation]]
   (let [sections 10]
     (range (- (+ (* sections ruler-step)
                  (rem (if (= orientation :vertical) y x)
                       (* sections ruler-step))))
            (if (= orientation :vertical) height width)
            ruler-step))))