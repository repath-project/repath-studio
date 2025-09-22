(ns renderer.app.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.events :as-alias document.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.error.events :as-alias error.events]
   [renderer.event.events :as-alias event.events]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.history.handlers :as history.handlers]
   [renderer.notification.events :as-alias notification.events]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.theme.events :as-alias theme.events]
   [renderer.utils.font :as utils.font]
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

(defonce listeners
  [[js/document "keydown" [::event.events/keyboard] event.impl.keyboard/->clj]
   [js/document "keyup" [::event.events/keyboard] event.impl.keyboard/->clj]
   [js/document "fullscreenchange" [::window.events/update-fullscreen]]
   [js/window "focus" [::window.events/update-focused]]
   [js/window "blur" [::window.events/update-focused]]])

(defonce ipc-listeners
  [["window-maximized" [::window.events/set-maximized true]]
   ["window-unmaximized" [::window.events/set-maximized false]]
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
    :fx (->>
         ipc-listeners
         (map (partial vector ::effects/ipc-on))
         (into [[::app.effects/get-local-db
                 {:on-success [::load-local-db]
                  :on-error [::notification.events/show-exception]
                  :on-finally [::db-loaded]}]]))}))

(rf/reg-event-fx
 ::load-local-db
 (fn [{:keys [db]} [_ local-stored-db]]
   (let [app-db (merge db local-stored-db)]
     (if (app.db/valid? app-db)
       {:db app-db}
       {::app.effects/clear-local-storage nil}))))

(rf/reg-event-db
 ::set-loading
 (fn [db [_ state]]
   (assoc db :loading state)))

(rf/reg-event-fx
 ::db-loaded
 [(rf/inject-cofx ::effects/guid)
  (rf/inject-cofx ::app.effects/language)]
 (fn [{:keys [db guid language]} _]
   (let [initial-document (:active-document db)]
     {:db (cond-> db
            :always
            (assoc :system-lang language)

            (not initial-document)
            (-> (document.handlers/create guid)
                (history.handlers/finalize [:create-doc "Create document"]))

            initial-document
            (snap.handlers/rebuild-tree))
      :fx (->> listeners
               (map (partial vector ::effects/add-event-listener))
               (into [[:dispatch [::error.events/init-reporting]]
                      [:dispatch [::theme.events/set-document-attr]]
                      [:dispatch [::set-lang-attrs]]
                      [::theme.effects/add-listener [::theme.events/set-document-attr]]
                      [:dispatch [::set-loading false]]
                      ;; We use flush-dom to render once so we can get the canvas size.
                      [:dispatch ^:flush-dom [::document.events/center]]
                      [:dispatch [::window.events/update-focused]]
                      [::effects/ipc-send ["initialized"]]]))})))

(rf/reg-event-db
 ::set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-fx
 ::set-lang-attrs
 (fn [{:keys [db]} _]
   (let [{:keys [lang system-lang]} db
         lang (utils.i18n/computed-lang lang system-lang)
         dir (get-in utils.i18n/languages [lang :dir])]
     {:fx [[::effects/set-document-attr ["lang" lang]]
           [::effects/set-document-attr ["dir" dir]]]})))

(rf/reg-event-fx
 ::set-lang
 [persist]
 (fn [{:keys [db]} [_ lang]]
   {:db (assoc db :lang lang)
    :dispatch [::set-lang-attrs]}))

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
     :formatter utils.font/font-data->system-fonts}}))

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
                (rf/assoc-effect :fx (conj (or fx [])
                                           [::app.effects/validate [db event]])))))))
