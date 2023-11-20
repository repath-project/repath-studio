(ns renderer.menubar.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 ::keydown
 (fn [id]
   (when-let [element (.getElementById js/document id)]
     (.focus element)
     (.dispatchEvent element
                     (js/KeyboardEvent. "keydown" #js {:keyCode 13
                                                       :bubbles true
                                                       :cancelable true})))))

(rf/reg-event-fx
 :menubar/toggle
 (fn [_ [_ id]]
   {::keydown id}))
