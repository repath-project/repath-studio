(ns renderer.notification.db
  (:require
   [renderer.utils.hiccup :refer [Hiccup]]))

(def Notification
  [:map {:closed true}
   [:count int?]
   [:content Hiccup]])
