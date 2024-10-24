(ns renderer.color.subs
  #_(:require
     [clojure.set :as set]
     [re-frame.core :as rf]
     [renderer.color.db :as color.db]))

#_(rf/reg-sub
   ::fills
   :<- [::element.s/visible]
   (fn [visible-elements _]
     (reduce (fn [colors element]
               (let [color (-> element :attrs :fill)]
                 (conj colors color)))
             #{}
             visible-elements)))

#_(rf/reg-sub
   ::selected-fills
   :<- [::element.s/selected]
   (fn [selected-elements _]
     (reduce (fn [colors element]
               (let [color (-> element :attrs :fill)]
                 (conj colors color)))
             #{}
             selected-elements)))

#_(rf/reg-sub
   ::custom-fills
   :<- [::fills]
   (fn [fills _]
     (let [palette-colors (set (flatten color.db/default-palette))
           colors (set/difference fills palette-colors)]
       (take 5 colors))))
