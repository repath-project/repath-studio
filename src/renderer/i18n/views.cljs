(ns renderer.i18n.views
  (:require
   [re-frame.core :as rf]
   [renderer.i18n.handlers :as i18n.handlers]
   [renderer.i18n.subs :as-alias i18n.subs]))

(defn t
  "Translation function that should be called in a reactive context."
  [& more]
  (let [lang @(rf/subscribe [::i18n.subs/lang])
        options @(rf/subscribe [::i18n.subs/options])]
    (i18n.handlers/translate options lang more)))
