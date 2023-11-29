(ns renderer.history.subs
  (:require
   [re-frame.core :as rf]
   [renderer.history.handlers :as h]))

(rf/reg-sub
 :history/undos?
 :<- [:document/history]
 (fn [history _] (h/undos? history)))

(rf/reg-sub
 :history/redos?
 :<- [:document/history]
 (fn [history _] (h/redos? history)))

(rf/reg-sub
 :history/undos
 :<- [:document/history]
 (fn [history _] (drop-last (h/undos history))))

(rf/reg-sub
 :history/redos
 :<- [:document/history]
 (fn [history _] (rest (h/redos history))))

#_(rf/reg-sub
   :history/step-count
   (fn [db _] (h/step-count db)))
