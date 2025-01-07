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
 ::bbox-rect-attrs
 :<- [::document.s/zoom]
 :<- [::document.s/pan]
 :<- [::element.s/bbox]
 :<- [::size]
 (fn [[zoom pan bbox size] [_ orientation]]
   (let [[min-x min-y max-x max-y] (map #(* % zoom) bbox)]
     (if (= orientation :vertical)
       {:x 0
        :y (- min-y (* (second pan) zoom))
        :width size
        :height (- max-y min-y)}
       {:x (- min-x (* (first pan) zoom))
        :y 0
        :width (- max-x min-x)
        :height size}))))
