(ns renderer.effects
  (:require
   [re-frame.core :as rf]
   [platform]))

(rf/reg-fx
 :send-to-main
 (fn [data]
   (when platform/electron?
     (js/window.api.send "toMain" (clj->js data)))))