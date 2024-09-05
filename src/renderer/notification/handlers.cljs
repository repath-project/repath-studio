(ns renderer.notification.handlers
  (:require
   [malli.experimental :as mx]
   [renderer.utils.hiccup :refer [Hiccup]]))

(mx/defn add
  [db, notification :- Hiccup]
  (let [notifications (:notifications db)]
    (if (= notification (-> notifications peek :content))
      (assoc db :notifications
             (conj (pop notifications) (update (peek notifications) :count inc)))
      (update db :notifications conj {:content notification}))))
