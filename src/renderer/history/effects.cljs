(ns renderer.history.effects
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
   [renderer.app.events :as-alias e]
   [renderer.history.handlers :as history.h]))

(def auto-persist
  "Persists the current state on history position changes."
  (rf/->interceptor
   :id ::auto-persist
   :after (fn [context]
            (let [db (rf/get-effect context :db)
                  fx (rf/get-effect context :fx)
                  prev-position (when-let [db (rf/get-coeffect context :db)]
                                  (when (:active-document db)
                                    (history.h/position db)))]
              (cond-> context
                (and db (not= (history.h/position db) prev-position))
                (rf/assoc-effect :fx (conj (or fx []) [::app.fx/persist db])))))))

(rf/reg-global-interceptor auto-persist)
