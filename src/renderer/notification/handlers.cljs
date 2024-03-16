(ns renderer.notification.handlers)

(defn add
  [db notification]
  (let [notifications (:notifications db)]
    (if (= notification (-> notifications peek :content))
      (assoc db :notifications
             (conj (pop notifications) (update (peek notifications) :count inc)))
      (update db :notifications conj {:content notification}))))
