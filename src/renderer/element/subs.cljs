(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.tool.base :as tool]
   [renderer.utils.attribute :as utils.attr]
   [renderer.utils.element :as utils.el]
   [renderer.utils.map :as map]
   [renderer.element.handlers :as h]))

(rf/reg-sub
 ::root
 (fn [db _]
   (h/root db)))

(rf/reg-sub
 ::root-children
 :<- [::document.s/elements]
 :<- [::root]
 (fn [[elements root] _]
   (mapv elements (:children root))))

(rf/reg-sub
 ::xml
 :<- [::root-children]
 (fn [root-children _]
   (-> (h/->string root-children)
       (js-beautify/html #js {:indent_size 2}))))

(rf/reg-sub
 ::filter-visible
 :<- [::document.s/elements]
 (fn [elements [_ ks]]
   (filter :visible? (mapv #(% elements) ks))))

(rf/reg-sub
 ::selected
 :<- [::document.s/elements]
 (fn [elements _]
   (filter :selected? (vals elements))))

(rf/reg-sub
 ::selected-descendant-keys
 (fn [db _]
   (h/descendant-keys db)))

(rf/reg-sub
 ::non-selected-visible
 :<- [::document.s/elements]
 :<- [::selected-descendant-keys]
 (fn [[elements selected-descendant-keys] _]
   (filter #(and (not (:selected? %))
                 (not (contains? selected-descendant-keys (:key %)))
                 (:visible? %)) (vals elements))))

(rf/reg-sub
 ::hovered
 :<- [::document.s/elements]
 :<- [::document.s/hovered-keys]
 (fn [[elements hovered-keys] _]
   (vals (select-keys elements hovered-keys))))

(rf/reg-sub
 ::selected-tags
 :<- [::selected]
 (fn [selected-elements _]
   (reduce #(conj %1 (:tag %2)) #{} selected-elements)))

(rf/reg-sub
 ::selected?
 :<- [::selected]
 (fn [selected-elements _]
   (seq selected-elements)))

(rf/reg-sub
 ::selected-locked?
 :<- [::selected]
 (fn [selected-elements _]
   (not-any? #(not (:locked? %)) selected-elements)))

(rf/reg-sub
 ::multiple-selected?
 :<- [::selected]
 (fn [selected-elements _]
   (seq (rest selected-elements))))

(rf/reg-sub
 ::selected-attrs
 :<- [::selected]
 :<- [::multiple-selected?]
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
                                tool/properties
                                :attrs
                                (.indexOf k)))
                          attrs))]
     (sort-by (fn [[k _]] (.indexOf utils.attr/order k)) attrs))))

(rf/reg-sub
 ::bounds
 :<- [::selected]
 (fn [selected-elements _]
   (utils.el/bounds selected-elements)))

(rf/reg-sub
 ::area
 :<- [::selected]
 (fn [selected-elements _]
   (reduce  #(+ %1 (tool/area %2)) 0 selected-elements)))

(rf/reg-sub
 ::ancestor-keys
 (fn [db _]
   (h/ancestor-keys db)))

(rf/reg-sub
 ::top-level?
 :<- [::root]
 :<- [::ancestor-keys]
 (fn [[root ancestor-keys] _]
   (empty? (disj (set ancestor-keys) (:key root)))))

