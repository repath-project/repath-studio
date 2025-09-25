(ns renderer.ruler.subs
  (:require
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
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
 ::step
 :<- [::document.subs/zoom]
 ruler.handlers/step)

(rf/reg-sub
 ::steps-coll
 :<- [::step]
 :<- [::frame.subs/viewbox]
 (fn [[step viewbox] [_ orientation]]
   (ruler.handlers/steps-coll step viewbox orientation)))
