(ns renderer.notification.db
  (:require
   [renderer.utils.hiccup :refer [Hiccup]]))

(def Notification
  [:map {:closed true}
   [:count {:optional true} int?]
   [:content Hiccup]])
