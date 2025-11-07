(ns renderer.panel.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 ::panels
 :-> :panels)

(rf/reg-sub
 ::visible?
 :<- [::panels]
 (fn [panels [_ id]]
   (get-in panels [id :visible])))
