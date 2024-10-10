(ns renderer.theme.effects
  (:require
   [re-frame.core :as rf]))

(def native-query! (.matchMedia js/window "(prefers-color-scheme: dark)"))

(defn native-mode!
  [query]
  (if (.-matches query) :dark :light))

(rf/reg-fx
 ::add-native-listener
 (fn [e]
   (.addListener native-query! #(rf/dispatch [e (native-mode! %)]))))
