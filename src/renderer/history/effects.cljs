(ns renderer.history.effects
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :as app.fx]
   [renderer.app.events :as-alias e]
   [renderer.history.handlers :as history.h]))

(def auto-persist
  (rf/->interceptor
   :id ::auto-persist
   :before (fn [context]
             (when-let [db (rf/get-coeffect context :db)]
               (cond-> context
                 (:active-document db)
                 (rf/assoc-coeffect :history-position (history.h/position db)))))
   :after (fn [context]
            (when-let [db (rf/get-effect context :db)]
              (when-let [history-position (rf/get-coeffect context :history-position)]
                (when (not= (history.h/position db) history-position)
                  (app.fx/persist! db))))
            context)))

(rf/reg-global-interceptor auto-persist)
