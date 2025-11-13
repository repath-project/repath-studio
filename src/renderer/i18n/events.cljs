(ns renderer.i18n.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.effects :as-alias effects]
   [renderer.i18n.handlers :as i18n.handlers]))

(rf/reg-event-fx
 ::set-lang-attrs
 (fn [{:keys [db]} _]
   (let [{:keys [languages user-lang system-lang]} db
         lang (i18n.handlers/computed-lang languages user-lang system-lang)
         dir (get-in languages [lang :dir])]
     {:fx [[::effects/set-document-attr ["lang" lang]]
           [::effects/set-document-attr ["dir" dir]]]})))

(rf/reg-event-fx
 ::set-user-lang
 [persist]
 (fn [{:keys [db]} [_ lang]]
   {:db (assoc db :user-lang lang)
    :dispatch [::set-lang-attrs]}))

(rf/reg-event-db
 ::register-language
 (fn [db [_ language]]
   (i18n.handlers/register-language db language)))

(rf/reg-event-db
 ::deregister-language
 (fn [db [_ id]]
   (i18n.handlers/deregister-language db id)))

(rf/reg-event-db
 ::set-translation
 (fn [db [_ lang-id k v]]
   (i18n.handlers/set-translation db lang-id k v)))
