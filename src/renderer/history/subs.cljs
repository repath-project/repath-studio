(ns renderer.history.subs
  (:require
   [re-frame.core :as rf]
   [renderer.history.handlers :as h]))

(rf/reg-sub
 :history/undos?
 (fn [db _]
   (h/undos? db)))

(rf/reg-sub
 :history/redos?
 (fn [db _]
   (h/redos? db)))

(rf/reg-sub
 :history/undos
 (fn [db _]
   (h/undos db)))

(rf/reg-sub
 :history/redos
 (fn [db _]
   (h/redos db)))

(defn state->d3-data
  [db id]
  (let [state (h/state db id)]
    #js {:name (:explanation state)
         :id id
         :active (= id (h/current-position db))
         :restored (:restored? state)
         :color (str "hsla(" (+ (* (/ 100 (h/state-count db)) (:index state)) 20) ",40%,60%,1)")
         :children (apply array (map #(state->d3-data db %) (:children state)))}))

(rf/reg-sub
 :history/tree-data
 (fn [db _]
   (let [root (:id (first (sort-by :index (vals (:states (h/history db))))))]
     (state->d3-data db root))))

(rf/reg-sub
 :history/state-count
 (fn [db _] (h/state-count db)))
