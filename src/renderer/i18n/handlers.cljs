(ns renderer.i18n.handlers
  (:require
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.i18n.db
    :refer [LanguageCodeIdentifier Language Languages LanguageId Translation]]
   [taoensso.tempura :as tempura]))

(m/=> register-language [:-> App Language App])
(defn register-language
  [db language]
  (assoc-in db [:languages (:id language)] language))

(m/=> register-language [:-> App LanguageCodeIdentifier App])
(defn deregister-language
  [db id]
  (update db :languages dissoc id))

(m/=> supported-lang? [:-> Languages [:maybe LanguageCodeIdentifier] boolean?])
(defn supported-lang?
  [languages lang]
  (contains? languages lang))

(m/=> computed-lang [:-> Languages LanguageId [:maybe LanguageCodeIdentifier]
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
