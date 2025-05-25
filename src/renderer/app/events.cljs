(ns renderer.app.events
  (:require
   [malli.error :as malli.error]
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.effects :as-alias effects]
   [renderer.notification.events :as-alias notification.events]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
   [renderer.utils.i18n :as utils.i18n]
   [renderer.utils.system :as utils.system]
   [renderer.window.effects :as-alias window.effects]))

(def persist
  (rf/->interceptor
   :id ::persist
   :after (fn [context]
            (let [db (rf/get-effect context :db)
                  fx (rf/get-effect context :fx)]
              (cond-> context
                db
                (rf/assoc-effect :fx (conj (or fx []) [::app.effects/persist])))))))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   app.db/default))

(rf/reg-event-fx
 ::load-local-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [app-db (merge db store)]
     (if (app.db/valid? app-db)
       {:db app-db}
       {::effects/local-storage-clear nil
        :db (notification.handlers/add db (notification.views/spec-failed
                                           "Invalid local configuration"
                                           (-> app-db
                                               app.db/explain
                                               malli.error/humanize
                                               str)))}))))

(rf/reg-event-db
 ::set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 ::set-lang
 (fn [db [_ lang]]
   (cond-> db
     (utils.i18n/lang? lang)
     (assoc :lang lang))))

(rf/reg-event-db
 ::set-repl-mode
 (fn [db [_ mode]]
   (assoc db :repl-mode mode)))

(rf/reg-event-db
 ::toggle-debug-info
 (fn [db [_]]
   (update db :debug-info not)))

(rf/reg-event-db
 ::toggle-help-bar
 (fn [db [_]]
   (update db :help-bar not)))

(rf/reg-event-db
 ::set-backdrop
 (fn [db [_ visible]]
   (assoc db :backdrop visible)))

(rf/reg-event-db
 ::toggle-grid
 [persist]
 (fn [db [_]]
   (update db :grid not)))

(rf/reg-event-db
 ::toggle-panel
 [persist]
 (fn [db [_ k]]
   (update-in db [:panels k :visible] not)))

(defn ->font-map
  [^js/FontData font-data]
  (into {} [[:postscriptName (.-postscriptName font-data)]
            [:fullName (.-fullName font-data)]
            [:family (.-family font-data)]
            [:style (.-style font-data)]]))

(rf/reg-event-fx
 ::load-system-fonts
 (fn [_ _]
   (if utils.system/electron?
     {::window.effects/ipc-invoke
      {:channel "load-system-fonts"
       :on-success [::set-system-fonts]
       :on-error [::notification.events/exception]
       :formatter #(js->clj % :keywordize-keys true)}}
     {::effects/query-local-fonts
      {:on-success [::set-system-fonts]
       :on-error [::notification.events/exception]
       :formatter #(mapv ->font-map %)}})))

(def schema-validator
  (rf/->interceptor
   :id ::schema-validator
   :after (fn [context]
            (let [db (or (rf/get-effect context :db)
                         (rf/get-coeffect context :db))
                  fx (rf/get-effect context :fx)
                  event (rf/get-coeffect context :event)]
              (cond-> context
                db
                (rf/assoc-effect :fx
                                 (conj (or fx [])
                                       [::app.effects/validate [db event]])))))))
