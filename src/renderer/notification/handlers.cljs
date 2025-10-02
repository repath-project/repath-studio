(ns renderer.notification.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.utils.hiccup :refer [Hiccup]]))

(m/=> add [:-> App Hiccup App])
(defn add
  [db notification]
  (let [notifications (:notifications db)]
    (if (= notification (-> notifications peek :content))
      (->> (update (peek notifications) :count inc)
           (conj (pop notifications))
           (assoc db :notifications))
      (update db :notifications conj {:content notification
                                      :count 1}))))
