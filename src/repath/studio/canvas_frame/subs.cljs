(ns repath.studio.canvas-frame.subs
  (:require
   [re-frame.core :as rf]
   [repath.studio.canvas-frame.handlers :as h]
   [clojure.core.matrix  :as matrix]))

(rf/reg-sub
 :canvas/viewbox
 :<- [:zoom]
 :<- [:pan]
 :<- [:content-rect]
 (fn [[zoom pan content-rect] _]
   (let [[x y] pan
         width  (/ (:width content-rect) zoom)
         height (/ (:height content-rect) zoom)]
     [x y width height])))

(rf/reg-sub
 :adjusted-mouse-pos
 :<- [:zoom]
 :<- [:pan]
 :<- [:mouse-pos]
 (fn [[zoom pan mouse-pos] _]
   (h/adjust-mouse-pos zoom pan mouse-pos)))

(rf/reg-sub
 :active-page-mouse-pos
 :<- [:elements/active-page]
 :<- [:adjusted-mouse-pos]
 (fn [[active-page-element adjusted-mouse-pos] _]
   (let [{:keys [x y]} (get-in active-page-element [:attrs])]
     (matrix/sub [x y] adjusted-mouse-pos))))
