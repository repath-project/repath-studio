(ns renderer.panel.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :panel/visible?
 (fn [db [_ key]]
   (-> db :panel key :visible?)))
