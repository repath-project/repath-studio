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

(rf/reg-sub
 :history/zoom
 (fn [db _]
   (get-in db [:documents (:active-document db) :history :zoom])))

(rf/reg-sub
 :history/translate
 (fn [db _]
   (get-in db [:documents (:active-document db) :history :translate])))

(defn state->d3-data
  [db id]
  (let [{:keys [index restored?] :as state} (h/state db id)
        count (h/state-count db)]
    #js {:name (:explanation state)
         :id id
         :active (= id (h/current-position db))
         :restored restored?
         :color (str "hsla(" (+ (* (/ 100 count) index) 20) ",40%,60%,1)")
         :children (apply array (map #(state->d3-data db %) (:children state)))}))

(rf/reg-sub
 :history/tree-data
 (fn [db _]
   (let [root (:id (first (sort-by :index (vals (:states (h/history db))))))]
     (state->d3-data db root))))
