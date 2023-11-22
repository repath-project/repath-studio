(ns renderer.effects
  (:require
   [platform]
   [re-frame.core :as rf]))

(rf/reg-fx
 :send-to-main
 (fn [data]
   (when platform/electron?
     (js/window.api.send "toMain" (clj->js data)))))
