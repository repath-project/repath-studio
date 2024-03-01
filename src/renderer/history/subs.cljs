(ns renderer.history.subs
  (:require
   [re-frame.core :as rf]
   [renderer.history.handlers :as h]))

(rf/reg-sub
 :history/history
 (fn [db _]
   (h/history db)))

(rf/reg-sub
 :history/undos?
 :<- [:history/history]
 (fn [history _]
   (h/undos? history)))

(rf/reg-sub
 :history/redos?
 :<- [:history/history]
 (fn [history _]
   (h/redos? history)))

(rf/reg-sub
 :history/undos
 :<- [:history/history]
 (fn [history _]
   (h/undos history)))

(rf/reg-sub
 :history/redos
 :<- [:history/history]
 (fn [history _]
   (h/redos history)))

(rf/reg-sub
 :history/zoom
 :<- [:history/history]
 :-> :zoom)

(rf/reg-sub
 :history/translate
 :<- [:history/history]
 :-> :translate)

(defn state->d3-data
  [history id save]
  (let [states (:states history)
        {:keys [index restored?] :as state} (get states id)
        count (count states)]
    #js {:name (:explanation state)
         :id id
         :saved (= id save)
         :active (= id (:position history))
         :restored restored?
         :color (str "hsla(" (+ (* (/ 100 count) index) 20) ",40%,60%,1)")
         :children (apply array (map #(state->d3-data history % save) (:children state)))}))

(rf/reg-sub
 :history/tree-data
 :<- [:history/history]
 :<- [:document/save]
 (fn [[history save] _]
   (let [root (:id (first (sort-by :index (vals (:states history)))))]
     (state->d3-data history root save))))
