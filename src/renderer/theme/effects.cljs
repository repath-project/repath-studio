(ns renderer.theme.effects
  (:require
   [re-frame.core :as rf]))

(def native-query! (.matchMedia js/window "(prefers-color-scheme: dark)"))

(rf/reg-cofx
 ::native-mode
 (fn [coeffects _]
   (assoc coeffects :native-mode (if (.-matches native-query!)
                                   :dark
                                   :light))))

(rf/reg-fx
 ::add-native-listener
 (fn [e]
   (.addListener native-query! #(rf/dispatch e))))
