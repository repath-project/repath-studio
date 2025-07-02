(ns renderer.app.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.event.events :as-alias event.events]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.history.handlers :as history.handlers]
   [renderer.notification.events :as-alias notification.events]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.theme.events :as-alias theme.events]
   [renderer.utils.i18n :as utils.i18n]
   [renderer.window.events :as-alias window.events]))

(def persist
  (rf/->interceptor
   :id ::persist
   :after (fn [context]
            (let [db (rf/get-effect context :db)
                  fx (rf/get-effect context :fx)]
              (cond-> context
                db
                (rf/assoc-effect :fx (conj (or fx []) [::app.effects/persist])))))))

(defonce document-listeners
  [[js/document "keydown" [::event.events/keyboard] event.impl.keyboard/->clj]
   [js/document "keyup" [::event.events/keyboard] event.impl.keyboard/->clj]
   [js/document "fullscreenchange" [::window.events/update-fullscreen]]
   [js/window "focus" [::window.events/update-focused]]
   [js/window "blur" [::window.events/update-focused]]])

(defonce ipc-listeners
  [["window-maximized" [::window.events/set-maximized true]]
   ["window-unmaximized" [::window.events/set-maximized false]]
   ["window-focused" [::window.events/set-focused true]]
   ["window-blurred" [::window.events/set-focused false]]
   ["window-entered-fullscreen" [::window.events/set-fullscreen true]]
   ["window-leaved-fullscreen" [::window.events/set-fullscreen false]]
   ["window-minimized" [::window.events/set-minimized true]]])

(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx ::app.effects/user-agent)
  (rf/inject-cofx ::app.effects/platform)
  (rf/inject-cofx ::app.effects/versions)
  (rf/inject-cofx ::app.effects/env)]
 (fn [{:keys [user-agent platform versions env]} _]
   {:db (assoc app.db/default
               :platform platform
               :versions (js->clj versions)
               :env (js->clj env)
               :user-agent user-agent)
    :fx (into
         [[::app.effects/get-local-db {:on-success [::load-local-db]
                                       :on-error [::notification.events/show-exception]
                                       :on-finally [::db-loaded]}]]
         (map (partial vector ::effects/ipc-on) ipc-listeners))}))

(rf/reg-event-fx
 ::load-local-db
 (fn [{:keys [db]} [_ local-stored-db]]
   (let [app-db (merge db local-stored-db)]
     (if (app.db/valid? app-db)
       {:db app-db}
       {::app.effects/clear-local-storage nil}))))

(rf/reg-event-fx
 ::db-loaded
 [(rf/inject-cofx ::effects/guid)
  (rf/inject-cofx ::app.effects/language)]
 (fn [{:keys [db guid language]} _]
   {:db (cond-> db
          (not (:lang db))
          (assoc :lang (if (utils.i18n/supported-lang? language)
                         language
                         "en-US"))

          (not (:active-document db))
          (-> (document.handlers/create guid)
              (history.handlers/finalize "Create document"))

          (:active-document db)
          (snap.handlers/rebuild-tree)

          :always
          (assoc :loading false))
    :fx (into
         [[:dispatch [::theme.events/set-document-attr]]
          [:dispatch ^:flush-dom [::set-document-attrs]]
          [:dispatch ^:flush-dom [::window.events/update-focused]]
          [::theme.effects/add-native-listener [::theme.events/set-document-attr]]
          [::effects/ipc-send ["initialized"]]]
         (map (partial vector ::effects/add-listener) document-listeners))}))

(rf/reg-event-db
 ::set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-fx
 ::set-document-attrs
 (fn [{:keys [db]} _]
   {:fx [[::effects/set-document-attr ["lang" (:lang db)]]
         [::effects/set-document-attr ["dir" (:dir db)]]]}))

(rf/reg-event-fx
 ::set-lang
 [persist]
 (fn [{:keys [db]} [_ lang]]
   {:db (cond-> db
          (utils.i18n/supported-lang? lang)
          (assoc :lang lang
                 :dir (get-in utils.i18n/languages [lang :dir])))
    :dispatch [::set-document-attrs]}))

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

(rf/reg-event-fx
 ::load-system-fonts
 (fn [_ _]
   {::app.effects/query-local-fonts
    {:on-success [::set-system-fonts]
     :on-error [::notification.events/show-exception]
     :formatter #(reduce (fn [fonts ^js/FontData font-data]
                           (let [family (.-family font-data)
                                 style (.-style font-data)]
                             (assoc-in fonts [family style]
                                       {:postscript-name (.-postscriptName font-data)
                                        :full-name (.-fullName font-data)})))
                         {} %)}}))

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
