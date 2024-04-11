(ns renderer.snap.subs
  (:require
   [kdtree]
   [re-frame.core :as rf]
   [renderer.utils.element :as utils.el]))

(rf/reg-sub
 :snap
 :-> :snap)

(rf/reg-sub
 :snap/enabled?
 :<- [:snap]
 (fn [snap _]
   (:enabled? snap)))

(rf/reg-sub
 :snap/options
 :<- [:snap]
 (fn [snap _]
   (:options snap)))

(rf/reg-sub
 :snap/nearest-neighbor
 :<- [:snap]
 (fn [snap _]
   (:nearest-neighbor snap)))

(rf/reg-sub
 :snap/points
 :<- [:element/non-selected-visible]
 :<- [:snap/enabled?]
 :<- [:snap/options]
 (fn [[non-selected-visible-elements enabled? options] _]
   (when enabled?
     (reduce (fn [points element]
               (apply conj points (utils.el/snapping-points element options)))
             [] non-selected-visible-elements))))

(rf/reg-sub
 :snap/tree
 :<- [:snap/points]
 (fn [snapping-points _]
   (kdtree/build-tree snapping-points)))

(rf/reg-sub
 :snap/in-viewport-tree
 :<- [:frame/viewbox]
 :<- [:snap/tree]
 (fn [[[x y width height] tree] _]
   (kdtree/build-tree (kdtree/interval-search tree [[x (+ x width)]
                                                    [y (+ y height)]]))))
