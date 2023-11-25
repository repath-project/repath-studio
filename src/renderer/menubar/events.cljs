(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::focus
 (fn [id]
   (when-let [element (.getElementById js/document id)]
     (.focus element))))

(rf/reg-event-fx
 :menubar/focus
 (fn [_ [_ id]]
   {::focus id}))
