(ns renderer.dev-preload
  "ClojureScript Function Instrumentation
   https://github.com/metosin/malli/blob/master/docs/clojurescript-function-instrumentation.md"
  {:dev/always true}
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.string :as str]
   [malli.core :as m]
   [malli.dev.cljs :as dev]
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.core]
   [renderer.utils.spec :as spec]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec"
  [db event]
  (when (not (m/validator app.db/app))
    (js/console.error (str "Event: " (first event)))
    (throw (js/Error. (str "Spec check failed: " (spec/explain db app.db/app))))))

(def schema-validator
  (rf/->interceptor
   :id :schema-validator
   :after (fn [context]
            (let [db (if (contains? (rf/get-effect context) :db)
                       (rf/get-effect context :db)
                       (rf/get-coeffect context :db))
                  event (rf/get-coeffect context :event)]
              (check-and-throw db event)
              context))))

(comment
  ;; Enable full db validation for debugging.
  (rf/reg-global-interceptor schema-validator)

  (rf/clear-global-interceptor :schema-validator)


  (dev/start!)

  (dev/stop!)

  (pprint (str/trim "This line suppresses some clj-kondo warnings.")))
