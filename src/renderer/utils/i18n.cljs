(ns renderer.utils.i18n
  "Internationalization namespace
   https://github.com/taoensso/tempura"
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [taoensso.tempura :as tempura :refer-macros [load-resource-at-compile-time]]))

;; We need to load resources at compile time in clojurescript
;; https://github.com/taoensso/tempura/issues/25#issuecomment-451742526
(def dictionary
  {"en-US" (load-resource-at-compile-time "lang/en-US.edn")
   "el-GR" (load-resource-at-compile-time "lang/el-GR.edn")})

(m/=> supported-lang? [:-> string? boolean?])
(defn supported-lang?
  [lang]
  (contains? dictionary lang))

(def opts {:dict dictionary})

(defn t
  "Translation function that should be called in a reactive context."
  [& more]
  (let [lang @(rf/subscribe [::app.subs/lang])]
    (apply tempura/tr opts [(or lang "en-US")] more)))
