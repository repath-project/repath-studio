(ns renderer.notification.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::entities
 :-> :notifications)
