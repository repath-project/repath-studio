(ns renderer.frame.subs
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.utils.pointer :as pointer]))

(rf/reg-sub
 ::viewbox
 :<- [::document.s/zoom]
 :<- [::document.s/pan]
 :<- [::app.s/dom-rect]
 (fn [[zoom pan {:keys [width height]}] _]
   (let [[x y] pan
         [width height] (mat/div [width height] zoom)]
     [x y width height])))

(rf/reg-sub
 ::adjusted-pointer-pos
 :<- [::document.s/zoom]
 :<- [::document.s/pan]
 :<- [::app.s/pointer-pos]
 (fn [[zoom pan pointer-pos] _]
   (pointer/adjust-position zoom pan pointer-pos)))
