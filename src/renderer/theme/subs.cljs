(ns renderer.theme.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::mode
 (fn [db _]
   (-> db :theme :mode)))
