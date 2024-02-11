(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [clojure.set :as set]
   [re-frame.core :as rf]
   [renderer.attribute.utils :as attr.utils]
   [renderer.tools.base :as tools]
   [renderer.utils.map :as map]
   [renderer.utils.bounds :as bounds]
   [renderer.element.handlers :as h]
   [renderer.element.utils :as el.utils]))

#_(rf/reg-sub
   :element/element
   :<- [:document/elements]
   (fn [elements [_ key]]
     (get elements key)))

(rf/reg-sub
 :element/canvas
 :<- [:document/elements]
 :-> :canvas)

(rf/reg-sub
 :element/pages
 :<- [:document/elements]
 :<- [:element/canvas]
 (fn [[elements canvas] _]
   (mapv elements (:children canvas))))

(rf/reg-sub
 :element/xml
 :<- [:element/pages]
 (fn [pages _]
   (js-beautify/html (h/->string pages) #js {:indent_size 2})))

#_(rf/reg-sub
   :element/filter
   :<- [:document/elements]
   (fn [elements [_ ks]]
     (mapv #(% elements) ks)))

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
 :element/selected-keys
 :<- [:element/selected]
 (fn [selected-elements _]
   (reduce #(conj %1 (:key %2)) #{} selected-elements)))

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

#_(rf/reg-sub
   :element/group-selected?
   :<- [:element/selected]
   (fn [selected-elements _]
     (seq (filter #(= (:tag %) :g) selected-elements))))

(rf/reg-sub
 :element/selected-attrs
 :<- [:element/selected]
 :<- [:element/multiple-selected?]
 (fn [[selected-elements multiple-selected?] _]
   (let [attrs (tools/attributes (first selected-elements))
         attrs (if multiple-selected?
                 (reduce
                  #(map/merge-common-with (fn [v1 v2] (if (= v1 v2) v1 nil))
                                          %1
                                          (tools/attributes %2))
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
   (tools/elements-bounds elements selected-elements)))

(rf/reg-sub
 :element/parent-page
 :<- [:document/elements]
 (fn [elements [_ el]]
   (el.utils/parent-page elements el)))

(rf/reg-sub
 :element/area
 :<- [:element/selected]
 (fn [selected-elements _]
   (reduce  #(+ %1 (tools/area %2)) 0 selected-elements)))

(rf/reg-sub
 :element/visible
 :<- [:document/elements]
 (fn [elements _]
   (filter :visible? (vals elements))))

(rf/reg-sub
 :element/hovered-or-selected
 :<- [:document/elements]
 :<- [:document/hovered-keys]
 :<- [:element/selected-keys]
 (fn [[elements hovered-keys selected-keys] _]
   (vals (select-keys elements (set/union hovered-keys selected-keys)))))

(rf/reg-sub
 :snapping-points
 :<- [:document/elements]
 :<- [:element/visible]
 (fn [[elements visible-elements] _]
   (reduce (fn [points element]
             (let [[x1 y1 x2 y2] (tools/adjusted-bounds element elements)
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
