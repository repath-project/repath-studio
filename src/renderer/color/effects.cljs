(ns renderer.color.effects
  (:require
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.e]))

(rf/reg-fx
 ::dropper
 (fn [{:keys [on-success on-error]}]
   (if (.-EyeDropper js/window)
     (-> (js/EyeDropper.)
         (.open)
         (.then on-success)
         (.catch on-error))
     (do (rf/dispatch [::notification.e/unavailable-feature
                       "EyeDropper"
                       "https://developer.mozilla.org/en-US/docs/Web/API/EyeDropper_API#browser_compatibility"])
         (rf/dispatch [:set-tool :select])))))
