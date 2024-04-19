(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :menubar/focus
 (fn [_ [_ id]]
   {:focus id}))
