(ns renderer.snap.subs
  (:require
   [kdtree]
   [re-frame.core :as rf]
   [renderer.utils.element :as utils.el]))

(rf/reg-sub
 :snap/points
 :<- [:document/elements]
 :<- [:element/non-selected-visible]
 :<- [:snap?]
 (fn [[elements non-selected-visible-elements snap?] _]
   (when snap?
     (reduce (fn [points element]
               (apply conj points (utils.el/snapping-points element elements)))
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
