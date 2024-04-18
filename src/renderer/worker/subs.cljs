(ns renderer.worker.subs
  (:require
   [re-frame.core :as rf]))

(rf/reg-sub
 :worker
 :-> :worker)

(rf/reg-sub
 :worker/tasks
 :<- [:worker]
 :-> :tasks)

(rf/reg-sub
 :worker/loading?
 :<- [:worker/tasks]
 (fn [tasks]
   (seq tasks)))
