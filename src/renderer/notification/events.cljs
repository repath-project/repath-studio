(ns renderer.notification.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.vec :as vec]))

(rf/reg-event-db
 :notification/add
 (fn [db [_ notification]]
   (let [notifications (:notifications db)]
     (if (= (:content notification) (-> notifications peek :content))
       (assoc db :notifications
              (conj (pop notifications) (update (peek notifications) :count inc)))
       (update db :notifications conj notification)))))

(rf/reg-event-db
 :notification/remove
 (fn [db [_ index]]
   (update db :notifications vec/remove-by-index index)))

(rf/reg-event-db
 :notification/clear-all
 (fn [db [_]]
   (assoc db :notifications [])))
