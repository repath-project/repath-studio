(ns renderer.color.effects
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.notification.events :as-alias notification.e]))

(rf/reg-fx
 ::dropper
 (fn [{:keys [on-success on-error]}]
   (-> (js/EyeDropper.)
       (.open)
       (.then (fn [color] (when on-success (rf/dispatch [on-success color]))))
       (.catch (fn [error] (when on-error (rf/dispatch [on-error error])))))))
