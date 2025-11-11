(ns renderer.i18n.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.i18n.db
    :refer [LanguageCodeIdentifier Languages Translation UserLanguage]]
   [taoensso.tempura :as tempura]))

(m/=> supported-lang? [:-> Languages [:maybe LanguageCodeIdentifier] boolean?])
(defn supported-lang?
  [languages lang]
  (contains? languages lang))

(m/=> computed-lang [:-> Languages UserLanguage [:maybe LanguageCodeIdentifier]
                     LanguageCodeIdentifier])
(defn computed-lang
  [languages user-lang system-lang]
  (if (or (not user-lang)
          (= user-lang "system"))
    (if (supported-lang? languages system-lang)
      system-lang
      "en-US")
    user-lang))

(m/=> tempura-options [:-> Languages map?])
(defn tempura-options
  [languages]
  {:dict (->> languages
              (map (fn [[k v]] [k (:dictionary v)]))
              (into {}))})

(m/=> translate [:-> map? LanguageCodeIdentifier Translation any?])
(defn translate
  [options lang translation]
  (apply tempura/tr options [lang] translation))

(m/=> t [:-> App [:* any?] any?])
(defn t
  "Translation function that can be called outside of a reactive context."
  [db & more]
  (let [{:keys [user-lang system-lang languages]} db
        lang (computed-lang languages user-lang system-lang)]
    (translate (tempura-options languages) lang more)))
