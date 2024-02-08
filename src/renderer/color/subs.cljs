(ns renderer.color.subs
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [renderer.color.db :as color.db]))

(rf/reg-sub
 :color/fills
 :<- [:element/visible]
 (fn [visible-elements _]
   (reduce (fn [colors element]
             (let [color (-> element :attrs :fill)]
               (conj colors color)))
           #{}
           visible-elements)))

(rf/reg-sub
 :color/selected-fills
 :<- [:element/selected]
 (fn [selected-elements _]
   (reduce (fn [colors element]
             (let [color (-> element :attrs :fill)]
               (conj colors color)))
           #{}
           selected-elements)))


(rf/reg-sub
 :color/custom-fills
 :<- [:color/fills]
 (fn [fills _]
   (let [palette-colors (set (flatten color.db/default-palette))
         colors (set/difference fills palette-colors)]
     (take 5 colors))))
