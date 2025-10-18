(ns renderer.theme.effects
  (:require
   [re-frame.core :as rf]))

(def native-query! (js/matchMedia "(prefers-color-scheme: dark)"))

(rf/reg-cofx
 ::native-mode
 (fn [coeffects _]
   (let [mode (if (.-matches native-query!) :dark :light)]
     (assoc coeffects :native-mode mode))))

(rf/reg-cofx
 ::theme-color
 (fn [coeffects _]
   (let [style (.getComputedStyle js/window (.-documentElement js/document))
         color (.getPropertyValue style "--primary")]
     (assoc coeffects :theme-color color))))

(rf/reg-fx
 ::add-listener
 (fn [e]
   (.addListener native-query! #(rf/dispatch e))))
