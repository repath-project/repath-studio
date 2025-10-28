(ns renderer.app.events
  (:require
   [cognitect.transit :as transit]
   [config :as config]
   [re-frame.core :as rf]
   [re-pressed.core :as re-pressed]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.app.events :as-alias app.events]
   [renderer.app.handlers :as app.handlers]
   [renderer.document.events :as-alias document.events]
   [renderer.document.handlers :as document.handlers]
   [renderer.effects :as-alias effects]
   [renderer.error.events :as-alias error.events]
   [renderer.event.events :as-alias event.events]
   [renderer.event.impl.keyboard :as impl.keyboard]
   [renderer.history.handlers :as history.handlers]
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
                (rf/assoc-effect :fx (conj (or fx [])
                                           [::app.effects/persist])))))))

(def ipc-listeners
  (->> [["window-maximized" [::window.events/set-maximized true]]
        ["window-unmaximized" [::window.events/set-maximized false]]
        ["window-entered-fullscreen" [::window.events/set-fullscreen true]]
        ["window-leaved-fullscreen" [::window.events/set-fullscreen false]]
        ["window-minimized" [::window.events/set-minimized true]]]
       (mapv (partial vector ::effects/ipc-on))))

(defn- json->clj
  [data]
  (let [reader (transit/reader :json)]
    (try (transit/read reader data)
         (catch :default err
           (rf/dispatch [::app.events/toast-error err])))))

(rf/reg-event-fx
 ::initialize
 [(rf/inject-cofx ::app.effects/user-agent)
  (rf/inject-cofx ::app.effects/platform)
  (rf/inject-cofx ::app.effects/versions)
  (rf/inject-cofx ::app.effects/env)
  (rf/inject-cofx ::app.effects/standalone)
  (rf/inject-cofx ::app.effects/features)
  (rf/inject-cofx ::app.effects/language)]
 (fn [{:keys [user-agent platform versions env standalone features language]} _]
   {:db (assoc app.db/default
               :platform platform
               :versions versions
               :env env
               :standalone standalone
               :user-agent user-agent
               :features features
               :system-lang language)
    :fx (into [[::app.effects/get-local-store
                {:store-key config/app-name
                 :formatter json->clj
                 :on-success [::load-local-db]
                 :on-error [::app.events/toast-error]
                 :on-finally [::db-loaded]}]]
              ipc-listeners)}))

(rf/reg-event-fx
 ::load-local-db
 (fn [{:keys [db]} [_ persisted-db]]
   (let [app-db (merge db persisted-db)]
     (if (app.db/valid? app-db)
       {:db app-db}
       {::app.effects/clear-local-store nil}))))

(rf/reg-event-db
 ::set-loading
 (fn [db [_ state]]
   (assoc db :loading state)))

(rf/reg-event-db
 ::set-install-prompt
 (fn [db [_ prompt]]
   (assoc db :install-prompt prompt)))

(rf/reg-event-fx
 ::install
 (fn [{:keys [db]} _]
   (when-let [install-prompt (:install-prompt db)]
     {::app.effects/install
      {:prompt install-prompt
       :outcomes {"dismissed" nil
                  "accepted" [::set-install-prompt nil]}}})))

(def listeners
  (->> [[js/document "keydown" [::event.events/keyboard] impl.keyboard/->clj]
        [js/document "keyup" [::event.events/keyboard] impl.keyboard/->clj]
        [js/document "fullscreenchange" [::window.events/update-fullscreen]]
        [js/window "focus" [::window.events/update-focused]]
        [js/window "blur" [::window.events/update-focused]]
        [js/window "resize" [::window.events/update-width]]
        [js/window "beforeinstallprompt" [::set-install-prompt]]]
       (mapv (partial vector ::effects/add-event-listener))))

(rf/reg-event-fx
 ::db-loaded
 [(rf/inject-cofx ::effects/guid)]
 (fn [{:keys [db guid]} _]
   {:db (if (:active-document db)
          (snap.handlers/rebuild-tree db)
          (-> db
              (document.handlers/create guid)
              (history.handlers/finalize [::create-doc "Create document"])))
    :fx (into [[:dispatch [::error.events/init-reporting]]
               ;; Initialize values that might flicker the view first.
               [:dispatch [::theme.events/set-document-attr]]
               [:dispatch [::theme.events/set-meta-color]]
               [:dispatch [::window.events/update-width]]
               [:dispatch [::set-lang-attrs]]
               [:dispatch [::set-loading false]]
               ;; We flush to render once so we can get the canvas size.
               [:dispatch ^:flush-dom [::document.events/center]]
               [:dispatch [::window.events/update-focused]]
               [::app.effects/hide-splash-screen]
               [::effects/ipc-send ["initialized"]]
               [::theme.effects/add-listener [::theme.events/set-document-attr]]
               [::app.effects/setup-paper]
               [:dispatch [::re-pressed/add-keyboard-event-listener "keydown"]]
               [:dispatch [::re-pressed/set-keydown-rules
                           impl.keyboard/keydown-rules]]]
              listeners)}))

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
     :on-error [::app.events/toast-error]
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
                (rf/assoc-effect :fx
                                 (conj (or fx [])
                                       [::app.effects/validate
                                        [db event]])))))))

(rf/reg-event-fx
 ::toast
 (fn [_ [_ toast-type title options]]
   {::app.effects/toast [toast-type title options]}))

(rf/reg-event-fx
 ::toast-error
 (fn [_ [_ ^js/Error error]]
   {::app.effects/toast [:error
                         (.-name error)
                         {:description (or (.-message error)
                                           (str error))}]}))
