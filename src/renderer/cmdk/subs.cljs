(ns renderer.cmdk.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :cmdk/visible?
 (fn [db _]
   (-> db :cmdk :visible?)))
