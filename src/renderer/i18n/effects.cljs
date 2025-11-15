(ns renderer.i18n.effects
  (:require
   [re-frame.core :as rf]))

(rf/reg-cofx
 ::language
 (fn [coeffects _]
   (assoc coeffects :language (.-language js/navigator))))
