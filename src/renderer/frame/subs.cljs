(ns renderer.frame.subs
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.frame.handlers :as handlers]))

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
 :frame/adjusted-mouse-pos
 :<- [:document/zoom]
 :<- [:document/pan]
 :<- [:mouse-pos]
 (fn [[zoom pan mouse-pos] _]
   (handlers/adjust-mouse-pos zoom pan mouse-pos)))
