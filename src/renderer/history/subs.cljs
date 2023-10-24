(ns renderer.history.subs
  (:require [re-frame.core :as rf]
            [renderer.history.handlers :as handlers]))

(rf/reg-sub
 :history/undos?
 :<- [:document/history]
 (fn [history _] (handlers/undos? history)))

(rf/reg-sub
 :history/redos?
 :<- [:document/history]
 (fn [history _] (handlers/redos? history)))

(rf/reg-sub
 :history/undos
 :<- [:document/history]
 (fn [history _] (drop-last (handlers/undos history))))

(rf/reg-sub
 :history/redos
 :<- [:document/history]
 (fn [history _] (rest (handlers/redos history))))

#_(rf/reg-sub
   :history/step-count
   (fn [db _] (handlers/step-count db)))