(ns repath.studio.rulers.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :rulers/step
 :<- [:zoom]
 (fn [zoom _]
   ;; Any attemt to ingeniously produce the following mapping was proven inferior.
   ;; Simply returning a fixed step depending on the zoom range works fine.
   ;; Zoom levels outside of this range are considered invalid for now.
   ;; Maybe we need to revisit this at some point.
   (condp > zoom
     0.001 2000
     0.025 1000
     0.05 500
     0.1 100
     0.25 50
     0.5 20
     1 10
     2.5 5
     5 4
     10 2
     25 1
     50 0.5
     0.1)))

(rf/reg-sub
 :rullers/steps-coll
 :<- [:rulers/step]
 :<- [:canvas/viewbox]
 (fn [[ruler-step [x y width height]] [_ orientation]]
   (range (- (+ (* 10 ruler-step) (rem (if (= orientation :vertical) y x) (* 10 ruler-step))))
          (if (= orientation :vertical) height width)
          ruler-step)))