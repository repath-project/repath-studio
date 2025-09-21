(ns renderer.error.events
  (:require
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.dialog.handlers :as dialog.handlers]
   [renderer.dialog.views :as dialog.views]
   [renderer.error.effects :as-alias error.effects]
   [renderer.menubar.views :as-alias menubar.views]
   [renderer.utils.i18n :refer [t tr]]))

(defn reporting-confirmation-dialog
  []
  (dialog.views/confirmation
   {:description (t [::reporting-description
                     [:div
                      [:p "Would you like to help us improve by sending anonymous error
                           reports? You can change your preference at any time from our
                           \"%1\" menu."]
                      [:p "For more information, please read our %2."]]]
                    [[:strong (t [::menubar.views/help "Help"])]
                     [:a.button-link
                      {:href "https://repath.studio/policies/privacy/"
                       :target "_blank"}
                      (t [::privacy-policy "privacy policy"])]])
    :confirm-action [::set-reporting true]
    :cancel-action [::set-reporting false]
    :cancel-label (t [::no-thank-you "No, thank you"])}))

(rf/reg-event-fx
 ::init-reporting
 (fn [{:keys [db]} _]
   (let [{:keys [error-reporting]} db]
     (if (nil? error-reporting)
       {:db (dialog.handlers/create db {:title (tr db
                                                   [::welcome "Welcome to %1!"]
                                                   [config/app-name])
                                        :close-button false
                                        :content [reporting-confirmation-dialog]
                                        :attrs {:onOpenAutoFocus #(.preventDefault %)}})}
       {::error.effects/init-reporting (-> config/sentry
                                           (assoc :enabled error-reporting)
                                           (clj->js))}))))

(rf/reg-event-fx
 ::set-reporting
 [persist]
 (fn [{:keys [db]} [_ enabled]]
   {:db (assoc db :error-reporting enabled)
    :fx [(when enabled
           [:dispatch [::init-reporting]])]}))

(rf/reg-event-fx
 ::toggle-reporting
 [persist]
 (fn [{:keys [db]} [_]]
   {:db (update db :error-reporting not)
    :dispatch [::init-reporting]}))
