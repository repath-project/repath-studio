(ns renderer.color.effects
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.notification.events :as-alias notification.e]))

(rf/reg-fx
 ::dropper
 (fn [{:keys [on-success]}]
   (-> (js/EyeDropper.)
       (.open)
       (.then (fn [^js/Object color] (rf/dispatch [on-success (.-sRGBHex color)])))
       (.catch (fn [error]
                 (rf/dispatch [::notification.e/add
                               [:div
                                [:h2.pb-4.font-bold "EyeDropper cannot be activated."]
                                [:div.text-error (str error)]]]))))))
