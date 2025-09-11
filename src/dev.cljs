(ns dev
  {:dev/always true}
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.string :as string]
   [malli.dev.cljs :as dev]
   [malli.dev.pretty :as m.dev.pretty]
   [re-frame.core :as rf]
   [renderer.app.events :as app.events]))

(comment
  ;; Enable full db validation
  (rf/reg-global-interceptor app.events/schema-validator)
  (rf/clear-global-interceptor ::app.events/schema-validator)

  ;; Enable function instrumentation
  ;; https://github.com/metosin/malli/blob/master/docs/clojurescript-function-instrumentation.md
  (dev/start! {:report (m.dev.pretty/reporter)})
  (dev/stop!)

  (pprint (string/trim "This line suppresses some clj-kondo warnings.")))
