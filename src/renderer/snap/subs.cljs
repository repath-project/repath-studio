(ns renderer.snap.subs
  (:require
   [kdtree :as kdtree]
   [re-frame.core :as rf]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.utils.element :as utils.el]))

(rf/reg-sub
 ::snap
 :-> :snap)

(rf/reg-sub
 ::enabled?
 :<- [::snap]
 (fn [snap _]
   (:enabled? snap)))

(rf/reg-sub
 ::options
 :<- [::snap]
 (fn [snap _]
   (:options snap)))

(rf/reg-sub
 ::nearest-neighbor
 :<- [::snap]
 (fn [snap _]
   (:nearest-neighbor snap)))

(rf/reg-sub
 ::points
 :<- [::element.s/non-selected-visible]
 :<- [::enabled?]
 :<- [::options]
 (fn [[non-selected-visible-elements enabled? options] _]
   (when enabled?
     (reduce (fn [points element]
               (apply conj points (utils.el/snapping-points element options)))
             [] non-selected-visible-elements))))

(rf/reg-sub
 ::tree
 :<- [::points]
 (fn [snapping-points _]
   (kdtree/build-tree snapping-points)))

(rf/reg-sub
 ::in-viewport-tree
 :<- [::frame.s/viewbox]
 :<- [::tree]
 (fn [[[x y width height] tree] _]
   (kdtree/build-tree (kdtree/interval-search tree [[x (+ x width)]
                                                    [y (+ y height)]]))))
