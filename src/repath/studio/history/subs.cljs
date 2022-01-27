(ns repath.studio.history.subs
  (:require [re-frame.core :as rf]
            [repath.studio.history.handlers :as handlers]))

(rf/reg-sub
 :history/undos?
 :<- [:history]
 (fn [history _] (handlers/undos? history)))

(rf/reg-sub
 :history/redos?
 :<- [:history]
 (fn [history _] (handlers/redos? history)))

(rf/reg-sub
 :history/undos
 :<- [:history]
 (fn [history _] (drop-last (handlers/undos history))))

(rf/reg-sub
 :history/redos
 :<- [:history]
 (fn [history _] (rest (handlers/redos history))))

(rf/reg-sub
 :history/step-count
 (fn [db _] (handlers/step-count db)))