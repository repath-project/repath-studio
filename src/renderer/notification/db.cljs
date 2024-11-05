(ns renderer.notification.db
  (:require
   [renderer.utils.hiccup :refer [Hiccup]]))

(def Notification
  [:map {:closed true}
   [:count pos-int?]
   [:content Hiccup]])
