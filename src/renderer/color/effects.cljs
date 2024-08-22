(ns renderer.color.effects
  (:require
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.e]))

(rf/reg-fx
 ::dropper
 (fn [{:keys [on-success]}]
   (if (.-EyeDropper js/window)
     (-> (js/EyeDropper.)
         (.open)
         (.then (fn [^js/Object color] (rf/dispatch [on-success (.-sRGBHex color)])))
         (.catch (fn [error]
                    (rf/dispatch [::notification.e/add
                                  [:div
                                   [:h2.pb-4.font-bold "EyeDropper cannot be activated."]
                                   [:div.text-error (str error)]]])
                    (rf/dispatch [:set-tool :select]))))
     (do (rf/dispatch [::notification.e/unavailable-feature
                       "EyeDropper"
                       "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"])
         (rf/dispatch [:set-tool :select])))))
