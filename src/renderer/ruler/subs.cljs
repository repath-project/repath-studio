(ns renderer.ruler.subs
  (:require
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.ruler.handlers :as h]))

(rf/reg-sub
 ::ruler
 :-> :ruler)

(rf/reg-sub
 ::locked?
 :<- [::ruler]
 :-> :locked)

(rf/reg-sub
 ::visible?
 :<- [::ruler]
 :-> :visible)

(rf/reg-sub
 ::size
 :<- [::ruler]
 :-> :size)

(rf/reg-sub
 ::step
 :<- [::document.s/zoom]
 h/step)

(rf/reg-sub
 ::steps-coll
 :<- [::step]
 :<- [::frame.s/viewbox]
 (fn [[step viewbox] [_ orientation]]
   (h/steps-coll step viewbox orientation)))

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
