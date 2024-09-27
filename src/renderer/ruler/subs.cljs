(ns renderer.ruler.subs
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]))

(rf/reg-sub
 ::ruler
 :-> :ruler)

(rf/reg-sub
 ::locked
 :<- [::ruler]
 :-> :locked)

(rf/reg-sub
 ::visible
 :<- [::ruler]
 :-> :visible)

(rf/reg-sub
 ::size
 :<- [::ruler]
 :-> :size)

(rf/reg-sub
 ::step
 :<- [::document.s/zoom]
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
 ::steps-coll
 :<- [::step]
 :<- [::frame.s/viewbox]
 (fn [[ruler-step [x y width height]] [_ orientation]]
   (let [sections 10]
     (range (- (+ (* sections ruler-step)
                  (rem (if (= orientation :vertical) y x)
                       (* sections ruler-step))))
            (if (= orientation :vertical) height width)
            ruler-step))))

(rf/reg-sub
 ::bounds-rect-attrs
 :<- [::document.s/zoom]
 :<- [::document.s/pan]
 :<- [::element.s/bounds]
 :<- [::size]
 (fn [[zoom pan bounds size] [_ orientation]]
   (let [[x1 y1 x2 y2] (map #(* % zoom) bounds)]
     (if (= orientation :vertical)
       {:x 0
        :y (- y1 (* (second pan) zoom))
        :width size
        :height (- y2 y1)}
       {:x (- x1 (* (first pan) zoom))
        :y 0
        :width (- x2 x1)
        :height size}))))
