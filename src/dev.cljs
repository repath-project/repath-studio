(ns dev
  {:dev/always true}
  (:require
   [clojure.pprint :refer (pprint)]
   [clojure.string :as string]
   [malli.dev.cljs :as dev]
   [malli.dev.cljs-kondo-preload :refer [send-kondo-config-to-shadow!]]
   [malli.dev.pretty :as m.dev.pretty]
   [re-frame.core :as rf]
   [renderer.app.events :as app.events]))

;; Enable full db validation
(rf/reg-global-interceptor app.events/schema-validator)

(send-kondo-config-to-shadow!)

(comment
  (rf/clear-global-interceptor ::app.events/schema-validator)

  ;; Enable function instrumentation
  ;; https://github.com/metosin/malli/blob/master/docs/clojurescript-function-instrumentation.md
  (dev/start! {:report (m.dev.pretty/reporter)})
  (dev/stop!)

  (pprint (string/trim "This line suppresses some clj-kondo warnings.")))
