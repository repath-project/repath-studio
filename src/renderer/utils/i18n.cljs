(ns renderer.utils.i18n
  "Internationalization namespace
   https://github.com/taoensso/tempura"
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [taoensso.tempura :refer [tr] :refer-macros [load-resource-at-compile-time]]))

(def dictionary
  "We need to load resources at compile time in clojurescript
   https://github.com/taoensso/tempura/issues/25#issuecomment-451742526"
  {:en-US (load-resource-at-compile-time "lang/en-US.edn")
   :el-GR (load-resource-at-compile-time "lang/el-GR.edn")})

(m/=> lang? [:-> keyword? boolean?])
(defn lang?
  [lang]
  (contains? dictionary lang))

(def opts {:dict dictionary})

(defn t
  "Custom translation function.
   Should be called in a reactive context."
  [& more]
  (let [lang @(rf/subscribe [::app.subs/lang])]
    (apply tr opts [lang] more)))
