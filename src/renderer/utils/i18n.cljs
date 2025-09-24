(ns renderer.utils.i18n
  "Internationalization namespace
   https://github.com/taoensso/tempura"
  (:require
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [taoensso.tempura
    :as tempura
    :refer-macros [load-resource-at-compile-time]]))

;; We need to load resources at compile time in clojurescript
;; https://github.com/taoensso/tempura/issues/25#issuecomment-451742526
(def languages
  {"en-US" {:dir "ltr"
            :native-name "English"
            :abbr "EN"
            :dictionary (load-resource-at-compile-time "lang/en-US.edn")}
   "es-ES" {:dir "ltr"
            :native-name "Español"
            :abbr "ES"
            :dictionary (load-resource-at-compile-time "lang/es-ES.edn")}
   "pt-PT" {:dir "ltr"
            :native-name "Português"
            :abbr "PT"
            :dictionary (load-resource-at-compile-time "lang/pt-PT.edn")}
   "ru-RU" {:dir "ltr"
            :native-name "Русский"
            :abbr "RU"
            :dictionary (load-resource-at-compile-time "lang/ru-RU.edn")}
   "zh-CN" {:dir "ltr"
            :native-name "中文（简体）"
            :abbr "ZH"
            :dictionary (load-resource-at-compile-time "lang/zh-CN.edn")}
   "fr-FR" {:dir "ltr"
            :native-name "Français"
            :abbr "FR"
            :dictionary (load-resource-at-compile-time "lang/fr-FR.edn")}
   "de-DE" {:dir "ltr"
            :native-name "Deutsch"
            :abbr "DE"
            :dictionary (load-resource-at-compile-time "lang/de-DE.edn")}
   "el-GR" {:dir "ltr"
            :native-name "Ελληνικά"
            :abbr "EL"
            :dictionary (load-resource-at-compile-time "lang/el-GR.edn")}
   "ar-EG" {:dir "rtl"
            :native-name "العربية (مصر)"
            :abbr "AR"
            :dictionary (load-resource-at-compile-time "lang/ar-EG.edn")}
   "ja-JP" {:dir "ltr"
            :native-name "日本語"
            :abbr "JA"
            :dictionary (load-resource-at-compile-time "lang/ja-JP.edn")}})

(m/=> supported-lang? [:-> [:maybe string?] boolean?])
(defn supported-lang?
  [lang]
  (contains? languages lang))

(def Lang
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value " is not a supported language"))}
   supported-lang?])

(m/=> computed-lang [:-> [:or Lang [:= "system"]] [:maybe string?] Lang])
(defn computed-lang
  [lang system-lang]
  (if (or (not lang) (= lang "system"))
    (if (supported-lang? system-lang)
      system-lang
      "en-US")
    lang))

(def options
  {:dict (->> languages
              (map (fn [[k v]] [k (:dictionary v)]))
              (into {}))})

(m/=> translate [:-> Lang [:* any?] any?])
(defn translate
  [lang args]
  (apply tempura/tr options [lang] args))

(defn tr
  "Translation function that can be called outside of a reactive context."
  [db & more]
  (let [{:keys [lang system-lang]} db
        lang (computed-lang lang system-lang)]
    (translate lang more)))

(defn t
  "Translation function that should be called in a reactive context."
  [& more]
  (let [lang @(rf/subscribe [::app.subs/computed-lang])]
    (translate lang more)))
