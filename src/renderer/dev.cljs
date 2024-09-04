(ns renderer.dev
  {:dev/always true}
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.string :as str]
   [malli.dev.cljs :as dev]
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.core]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec"
  [db event]
  (when (not (app.db/valid? db))
    (js/console.error (str "Event: " (first event)))
    (throw (js/Error. (str "Spec check failed: " (app.db/explain db))))))

(def schema-validator
  (rf/->interceptor
   :id ::schema-validator
   :after (fn [context]
            (let [db (if (contains? (rf/get-effect context) :db)
                       (rf/get-effect context :db)
                       (rf/get-coeffect context :db))
                  event (rf/get-coeffect context :event)]
              (check-and-throw db event)
              context))))

(comment
  ;; Enable full db validation
  (rf/reg-global-interceptor schema-validator)
  (rf/clear-global-interceptor ::schema-validator)

  ;; Enable function instrumentation
  ;; https://github.com/metosin/malli/blob/master/docs/clojurescript-function-instrumentation.md
  (dev/start!)
  (dev/stop!)

  (pprint (str/trim "This line suppresses some clj-kondo warnings.")))
