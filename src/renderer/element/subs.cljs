(ns renderer.element.subs
  (:require
   ["js-beautify" :as js-beautify]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as h]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.attribute :as attr]
   [renderer.utils.element :as element]
   [renderer.utils.map :as utils.map]))

(rf/reg-sub
 ::root
 h/root)

(rf/reg-sub
 ::root-children
 :<- [::document.s/elements]
 :<- [::root]
 (fn [[elements root] _]
   (mapv elements (:children root))))

(rf/reg-sub
 ::entity
 :<- [::document.s/elements]
 (fn [elements [_ id]]
   (get elements id)))

(rf/reg-sub
 ::xml
 :<- [::root-children]
 (fn [root-children _]
   (-> (element/->string root-children)
       (js-beautify/html #js {:indent_size 2}))))

(rf/reg-sub
 ::filter-visible
 :<- [::document.s/elements]
 (fn [elements [_ ks]]
   (filter :visible (mapv #(get elements %) ks))))

(rf/reg-sub
 ::selected
 :<- [::document.s/elements]
 (fn [elements _]
   (filter :selected (vals elements))))

(rf/reg-sub
 ::hovered
 :<- [::document.s/elements]
 :<- [::document.s/hovered-ids]
 (fn [[elements hovered-ids] _]
   (vals (select-keys elements hovered-ids))))

(rf/reg-sub
 ::selected-tags
 :<- [::selected]
 (fn [selected-elements _]
   (->> selected-elements (map :tag) set)))

(rf/reg-sub
 ::some-selected
 :<- [::selected]
 seq)

(rf/reg-sub
 ::selected-locked
 :<- [::selected]
 (fn [selected-elements _]
   (not-any? #(not (:locked %)) selected-elements)))

(rf/reg-sub
 ::multiple-selected
 :<- [::selected]
 (fn [selected-elements _]
   (seq (rest selected-elements))))

(rf/reg-sub
 ::selected-attrs
 :<- [::selected]
 :<- [::multiple-selected]
 (fn [[selected-elements multiple-selected] _]
   (when (seq selected-elements)
     (let [attrs (->> selected-elements
                      (map element/attributes)
                      (apply utils.map/merge-common-with
                             (fn [v1 v2] (if (= v1 v2) v1 nil))))
           attrs (if multiple-selected
                   (dissoc attrs :id)
                   (sort-by (fn [[id _]]
                              (-> (first selected-elements)
                                  (element/properties)
                                  :attrs
                                  (.indexOf id)))
                            (element/attributes (first selected-elements))))]
       (sort-by (fn [[id _]] (.indexOf attr/order id)) attrs)))))

(rf/reg-sub
 ::bounds
 :<- [::selected]
 (fn [selected-elements _]
   (element/united-bounds selected-elements)))

(rf/reg-sub
 ::area
 :<- [::selected]
 (fn [selected-elements _]
   (reduce  #(+ %1 (hierarchy/area %2)) 0 selected-elements)))

(rf/reg-sub
 ::ancestor-ids
 (fn [db _]
   (h/ancestor-ids db)))

(rf/reg-sub
 ::font-weights
 :<- [::selected]
 :<- [::app.s/system-fonts]
 (fn [[selected-elements system-fonts] _]
   (let [families (->> selected-elements
                       (map #(-> % :attrs :font-family))
                       (remove nil?)
                       (set))]
     (->> system-fonts
          (filter #(contains? families (:family %)))
          (map :weight)
          (remove nil?)
          (distinct)
          (sort)
          (vec)))))

(rf/reg-sub
 ::every-top-level
 :<- [::root]
 :<- [::ancestor-ids]
 (fn [[root ancestor-ids] _]
   (empty? (disj (set ancestor-ids) (:id root)))))

