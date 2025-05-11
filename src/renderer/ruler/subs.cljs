(ns renderer.ruler.subs
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.subs :as-alias element.subs]
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.ruler.handlers :as ruler.handlers]))

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
 :<- [::document.subs/zoom]
 ruler.handlers/step)

(rf/reg-sub
 ::steps-coll
 :<- [::step]
 :<- [::frame.subs/viewbox]
 (fn [[step viewbox] [_ orientation]]
   (ruler.handlers/steps-coll step viewbox orientation)))

(rf/reg-sub
 ::bbox-rect-attrs
 :<- [::document.subs/zoom]
 :<- [::document.subs/pan]
 :<- [::element.subs/bbox]
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
