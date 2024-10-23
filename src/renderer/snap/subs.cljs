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
 ::active
 :<- [::snap]
 :-> :active)

(rf/reg-sub
 ::options
 :<- [::snap]
 :-> :options)

(rf/reg-sub
 ::nearest-neighbor
 :<- [::snap]
 :-> :nearest-neighbor)

(rf/reg-sub
 ::points
 :<- [::element.s/non-selected-visible]
 :<- [::active]
 :<- [::options]
 (fn [[non-selected-visible-elements active options] _]
   (when active
     (reduce (fn [points el] (into points (utils.el/snapping-points el options)))
             [] non-selected-visible-elements))))

(rf/reg-sub
 ::tree
 :<- [::points]
 kdtree/build-tree)

(rf/reg-sub
 ::in-viewport-tree
 :<- [::frame.s/viewbox]
 :<- [::tree]
 (fn [[[x y width height] tree] _]
   (kdtree/build-tree (kdtree/interval-search tree [[x (+ x width)]
                                                    [y (+ y height)]]))))
