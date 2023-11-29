(ns renderer.frame.subs
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.frame.handlers :as h]))

(rf/reg-sub
 :frame/viewbox
 :<- [:document/zoom]
 :<- [:document/pan]
 :<- [:content-rect]
 (fn [[zoom pan {:keys [width height]}] _]
   (let [[x y] pan
         [width height] (mat/div [width height] zoom)]
     [x y width height])))

(rf/reg-sub
 :frame/adjusted-pointer-pos
 :<- [:document/zoom]
 :<- [:document/pan]
 :<- [:pointer-pos]
 (fn [[zoom pan pointer-pos] _]
   (h/adjust-pointer-pos zoom pan pointer-pos)))
