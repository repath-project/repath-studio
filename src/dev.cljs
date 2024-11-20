(ns dev
  {:dev/always true}
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.string :as str]
   [malli.dev.cljs :as dev]
   [re-frame.core :as rf]
   [renderer.app.events :as app.e]))

(comment
  ;; Enable full db validation
  (rf/reg-global-interceptor app.e/schema-validator)
  (rf/clear-global-interceptor ::app.e/schema-validator)

  ;; Enable function instrumentation
  ;; https://github.com/metosin/malli/blob/master/docs/clojurescript-function-instrumentation.md
  (dev/start!)
  (dev/stop!)

  (pprint (str/trim "This line suppresses some clj-kondo warnings.")))
