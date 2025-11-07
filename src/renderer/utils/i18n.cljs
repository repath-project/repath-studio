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
            :locale "English"
            :code "EN"
            :dictionary (load-resource-at-compile-time "lang/en-US.edn")}
   "es-ES" {:dir "ltr"
            :locale "Español"
            :code "ES"
            :dictionary (load-resource-at-compile-time "lang/es-ES.edn")}
   "pt-PT" {:dir "ltr"
            :locale "Português"
            :code "PT"
            :dictionary (load-resource-at-compile-time "lang/pt-PT.edn")}
   "ru-RU" {:dir "ltr"
            :locale "Русский"
            :code "RU"
            :dictionary (load-resource-at-compile-time "lang/ru-RU.edn")}
   "zh-CN" {:dir "ltr"
            :locale "中文（简体）"
            :code "ZH"
            :dictionary (load-resource-at-compile-time "lang/zh-CN.edn")}
   "fr-FR" {:dir "ltr"
            :locale "Français"
            :code "FR"
            :dictionary (load-resource-at-compile-time "lang/fr-FR.edn")}
   "de-DE" {:dir "ltr"
            :locale "Deutsch"
            :code "DE"
            :dictionary (load-resource-at-compile-time "lang/de-DE.edn")}
   "el-GR" {:dir "ltr"
            :locale "Ελληνικά"
            :code "EL"
            :dictionary (load-resource-at-compile-time "lang/el-GR.edn")}
   "ar-EG" {:dir "rtl"
            :locale "العربية (مصر)"
            :code "AR"
            :dictionary (load-resource-at-compile-time "lang/ar-EG.edn")}
   "ja-JP" {:dir "ltr"
            :locale "日本語"
            :code "JA"
            :dictionary (load-resource-at-compile-time "lang/ja-JP.edn")}
   "ko-KR" {:dir "ltr"
            :locale "한국어"
            :code "KO"
            :dictionary (load-resource-at-compile-time "lang/ko-KR.edn")}
   "tr-TR" {:dir "ltr"
            :locale "Türkçe"
            :code "TR"
            :dictionary (load-resource-at-compile-time "lang/tr-TR.edn")}
   "it-IT" {:dir "ltr"
            :locale "Italiano"
            :code "IT"
            :dictionary (load-resource-at-compile-time "lang/it-IT.edn")}
   "nl-NL" {:dir "ltr"
            :locale "Nederlands"
            :code "NL"
            :dictionary (load-resource-at-compile-time "lang/nl-NL.edn")}
   "sv-SE" {:dir "ltr"
            :locale "Svenska"
            :code "SV"
            :dictionary (load-resource-at-compile-time "lang/sv-SE.edn")}})

(m/=> supported-lang? [:-> [:maybe string?] boolean?])
(defn supported-lang?
  [lang]
  (contains? languages lang))

(def Lang
  [:fn {:error/fn (fn [{:keys [value]} _]
                    (str value " is not a supported language"))}
   supported-lang?])

(def Translation [:* any?])

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

(m/=> translate [:-> Lang Translation any?])
(defn translate
  [lang translation]
  (apply tempura/tr options [lang] translation))

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
