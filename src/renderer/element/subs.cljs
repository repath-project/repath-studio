(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   #_[clojure.core.matrix :as mat]
   [clojure.set :as set]
   [goog.color :as color]
   [re-frame.core :as rf]
   [renderer.attribute.utils :as attr-utils]
   [renderer.tools.base :as tools]
   [renderer.utils.map :as map]))

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
 :element/active-page
 :<- [:document/active-page]
 :<- [:document/elements]
 (fn [[active-page elements] _]
   (get elements active-page)))

(rf/reg-sub
 :element/xml
 :<- [:element/active-page]
 (fn [active-page _]
   (js-beautify/html (tools/render-to-string active-page) #js {:indent_size 2})))

(rf/reg-sub
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
                  attrs
                  (rest selected-elements))

                 (sort-by (fn [[k _]]
                            (-> (first selected-elements)
                                :tag
                                (tools/properties)
                                :attrs
                                (.indexOf k)))
                          attrs))]
     (sort-by (fn [[k _]] (.indexOf attr-utils/attrs-order k)) attrs))))

(rf/reg-sub
 :element/bounds
 :<- [:document/elements]
 :<- [:element/selected]
 (fn [[elements selected-elements] _]
   (tools/elements-bounds elements selected-elements)))

(rf/reg-sub
 :element/area
 :<- [:element/selected]
 (fn [selected-elements _]
   (reduce (fn [area element] (+ (tools/area element) area))
           0
           selected-elements)))

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
 :element/colors
 :<- [:element/visible]
 (fn [visible-elements _]
   (reduce (fn [colors element]
             (let [color (get-in element [:attrs :fill])]
               (if (and color (color/isValidColor color))
                 (conj colors (keyword (:hex (color/parse color))))
                 colors)))
           #{}
           visible-elements)))

#_(rf/reg-sub
   :snaping-points
   :<- [:document/elements]
   :<- [:element/visible]
   (fn [elements visible-elements _]
     (reduce (fn [points element]
               (let [[x1 y1 x2 y2] (tools/adjusted-bounds element elements)
                     [width height] (mat/sub [x2 y2] [x1 y1])]
                 (concat points [[x1 y1]
                                 [x1 y2]
                                 [x1 (+ y1 (/ height 2))]
                                 [x2 y1]
                                 [(+ x1 (/ width 2)) y1]
                                 [x2 y2]
                                 [(+ x1 (/ width 2)) (+ y1 (/ height 2))]])))
             []
             visible-elements)))
