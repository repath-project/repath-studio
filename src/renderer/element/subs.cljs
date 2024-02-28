(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.utils :as attr.utils]
   [renderer.tools.base :as tools]
   [renderer.utils.element :as utils.el]
   [renderer.utils.map :as map]
   [renderer.utils.bounds :as bounds]
   [renderer.element.handlers :as h]))

(rf/reg-sub
 :element/canvas
 :<- [:document/elements]
 :-> :canvas)

(rf/reg-sub
 :element/canvas-children
 :<- [:document/elements]
 :<- [:element/canvas]
 (fn [[elements canvas] _]
   (mapv elements (:children canvas))))

(rf/reg-sub
 :element/xml
 :<- [:element/canvas-children]
 (fn [canvas-children _]
   (-> (h/->string canvas-children)
       (js-beautify/html #js {:indent_size 2}))))

(rf/reg-sub
 :element/filter-visible
 :<- [:document/elements]
 (fn [elements [_ ks]]
   (filter :visible? (mapv #(% elements) ks))))

(rf/reg-sub
 :element/selected
 :<- [:document/elements]
 (fn [elements _]
   (filter :selected? (vals elements))))

(rf/reg-sub
 :element/hovered
 :<- [:document/elements]
 :<- [:document/hovered-keys]
 (fn [[elements hovered-keys] _]
   (vals (select-keys elements hovered-keys))))

(rf/reg-sub
 :element/selected-tags
 :<- [:element/selected]
 (fn [selected-elements _]
   (reduce #(conj %1 (:tag %2)) #{} selected-elements)))

(rf/reg-sub
 :element/selected?
 :<- [:element/selected]
 (fn [selected-elements _]
   (seq selected-elements)))

(rf/reg-sub
 :element/selected-locked?
 :<- [:element/selected]
 (fn [selected-elements _]
   (not-any? #(not (:locked? %)) selected-elements)))

(rf/reg-sub
 :element/multiple-selected?
 :<- [:element/selected]
 (fn [selected-elements _]
   (seq (rest selected-elements))))


(rf/reg-sub
 :element/selected-attrs
 :<- [:element/selected]
 :<- [:element/multiple-selected?]
 (fn [[selected-elements multiple-selected?] _]
   (let [attrs (utils.el/attributes (first selected-elements))
         attrs (if multiple-selected?
                 (reduce
                  #(map/merge-common-with (fn [v1 v2] (if (= v1 v2) v1 nil))
                                          %1
                                          (utils.el/attributes %2))
                  (dissoc attrs :id)
                  (rest selected-elements))

                 (sort-by (fn [[k _]]
                            (-> (first selected-elements)
                                :tag
                                tools/properties
                                :attrs
                                (.indexOf k)))
                          attrs))]
     (sort-by (fn [[k _]] (.indexOf attr.utils/attrs-order k)) attrs))))

(rf/reg-sub
 :element/bounds
 :<- [:document/elements]
 :<- [:element/selected]
 (fn [[elements selected-elements] _]
   (utils.el/bounds elements selected-elements)))

(rf/reg-sub
 :element/el-bounds
 :<- [:document/elements]
 (fn [elements [_ el]]
   (utils.el/bounds elements [el])))

(rf/reg-sub
 :element/el-offset
 :<- [:document/elements]
 (fn [elements [_ el]]
   (let [bounds (utils.el/bounds elements [el])
         original-bounds (tools/bounds el)]
     (take 2 (mat/sub bounds original-bounds)))))

(rf/reg-sub
 :element/area
 :<- [:element/selected]
 (fn [selected-elements _]
   (reduce  #(+ %1 (tools/area %2)) 0 selected-elements)))

(rf/reg-sub
 :element/ancestor-keys
 (fn [db _]
   (h/ancestor-keys db)))

(rf/reg-sub
 :element/top-level?
 :<- [:element/ancestor-keys]
 (fn [ancestor-keys _]
   (empty? (disj ancestor-keys :canvas))))

(rf/reg-sub
 :element/visible
 :<- [:document/elements]
 (fn [elements _]
   (filter :visible? (vals elements))))

(rf/reg-sub
 :snapping-points
 :<- [:document/elements]
 :<- [:element/visible]
 (fn [[elements visible-elements] _]
   (reduce (fn [points element]
             (let [[x1 y1 x2 y2] (utils.el/adjusted-bounds element elements)
                   [cx cy] (bounds/center [x1 y1 x2 y2])]
               (conj points
                     [x1 y1]
                     [x1 y2]
                     [x1 cy]
                     [x2 y1]
                     [x2 y2]
                     [x2 cy]
                     [cx y1]
                     [cx y2]
                     [cx cy]))) [] visible-elements)))
