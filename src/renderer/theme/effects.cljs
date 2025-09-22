(ns renderer.theme.effects
  (:require
   [re-frame.core :as rf]))

(def native-query! (.matchMedia js/window "(prefers-color-scheme: dark)"))

(rf/reg-cofx
 ::native-mode
 (fn [coeffects _]
   (let [mode (if (.-matches native-query!) :dark :light)]
     (assoc coeffects :native-mode mode))))

(rf/reg-fx
 ::add-listener
 (fn [e]
   (.addListener native-query! #(rf/dispatch e))))
