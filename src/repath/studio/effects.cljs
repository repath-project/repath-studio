(ns repath.studio.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-fx
 :send-to-main
 (fn [data]
   (js/window.api.send "toMain" (clj->js data))))