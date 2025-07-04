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
(def languages
  {"en-US" {:dir "ltr"
            :native-name "English"
            :dictionary (load-resource-at-compile-time "lang/en-US.edn")}
   "es-ES" {:dir "ltr"
            :native-name "Español"
            :dictionary (load-resource-at-compile-time "lang/es-ES.edn")}
   "pt-PT" {:dir "ltr"
            :native-name "Português"
            :dictionary (load-resource-at-compile-time "lang/pt-PT.edn")}
   "ru-RU" {:dir "ltr"
            :native-name "Русский"
            :dictionary (load-resource-at-compile-time "lang/ru-RU.edn")}
   "zh-CN" {:dir "ltr"
            :native-name "中文（简体）"
            :dictionary (load-resource-at-compile-time "lang/zh-CN.edn")}
   "fr-FR" {:dir "ltr"
            :native-name "Français"
            :dictionary (load-resource-at-compile-time "lang/fr-FR.edn")}
   "de-DE" {:dir "ltr"
            :native-name "Deutsch"
            :dictionary (load-resource-at-compile-time "lang/de-DE.edn")}
   "el-GR" {:dir "ltr"
            :native-name "Ελληνικά"
            :dictionary (load-resource-at-compile-time "lang/el-GR.edn")}
   "ar-EG" {:dir "rtl"
            :native-name "العربية (مصر)"
            :dictionary (load-resource-at-compile-time "lang/ar-EG.edn")}})

(m/=> supported-lang? [:-> string? boolean?])
(defn supported-lang?
  [lang]
  (contains? languages lang))

(def options
  {:dict (into {} (map (fn [[k v]] [k (:dictionary v)])) languages)})

(defn t
  "Translation function that should be called in a reactive context."
  [& more]
  (let [lang @(rf/subscribe [::app.subs/lang])]
    (apply tempura/tr options [(or lang "en-US")] more)))
