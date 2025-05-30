(ns renderer.app.events
  (:require
   [malli.error :as malli.error]
   [re-frame.core :as rf]
   [renderer.app.db :as app.db]
   [renderer.app.effects :as-alias app.effects]
   [renderer.effects :as-alias effects]
   [renderer.event.events :as-alias event.events]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.notification.events :as-alias notification.events]
   [renderer.notification.handlers :as notification.handlers]
   [renderer.notification.views :as notification.views]
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

(rf/reg-event-fx
 ::initialize-db
 [(rf/inject-cofx ::app.effects/user-agent)
  (rf/inject-cofx ::app.effects/platform)
  (rf/inject-cofx ::app.effects/system-language)]
 (fn [{:keys [user-agent platform system-language]} _]
   {:db (assoc app.db/default
               :platform platform
               :user-agent user-agent
               :system-lang system-language)}))

(rf/reg-event-fx
 ::load-local-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [app-db (merge db store)]
     (if (app.db/valid? app-db)
       {:db app-db}
       {::app.effects/clear-local-storage nil
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

(rf/reg-event-fx
 ::set-document-lang
 (fn [{:keys [db]} _]
   {::effects/set-document-attr ["lang" (:lang db)]}))

(rf/reg-event-fx
 ::set-lang
 [persist]
 (fn [{:keys [db]} [_ lang]]
   {:db (cond-> db
          (utils.i18n/supported-lang? lang)
          (assoc :lang lang))
    :dispatch [::set-document-lang]}))

(rf/reg-event-fx
 ::init-lang
 [persist]
 (fn [{:keys [db]} _]
   {:db (cond-> db
          (not (:lang db))
          (assoc :lang (if (utils.i18n/supported-lang? (:system-lang db))
                         (:system-lang db)
                         "en-US")))
    :dispatch [::set-document-lang]}))

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
 (fn [{:keys [db]} _]
   (if (= (:platform db) "web")
     {::app.effects/query-local-fonts
      {:on-success [::set-system-fonts]
       :on-error [::notification.events/show-exception]
       :formatter #(mapv ->font-map %)}}
     {::effects/ipc-invoke
      {:channel "load-system-fonts"
       :on-success [::set-system-fonts]
       :on-error [::notification.events/show-exception]
       :formatter #(js->clj % :keywordize-keys true)}})))

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

(rf/reg-event-fx
 ::add-listeners
 (fn [_ _]
   {:fx (->> [[js/document "keydown" [::event.events/keyboard] event.impl.keyboard/->clj]
              [js/document "keyup" [::event.events/keyboard] event.impl.keyboard/->clj]
              [js/document "fullscreenchange" [::window.events/update-fullscreen]]
              [js/window "load" [::window.events/update-focused]]
              [js/window "focus" [::window.events/update-focused]]
              [js/window "blur" [::window.events/update-focused]]]
             (mapv #(vector ::effects/add-listener %)))}))

(rf/reg-event-fx
 ::register-listeners
 (fn [{:keys [db]} _]
   (if (= (:platform db) "web")
     {:dispatch [::add-listeners]}
     {:fx (->> [["window-maximized" [::window.events/set-maximized true]]
                ["window-unmaximized" [::window.events/set-maximized false]]
                ["window-focused" [::window.events/set-focused true]]
                ["window-blurred" [::window.events/set-focused false]]
                ["window-entered-fullscreen" [::window.events/set-fullscreen true]]
                ["window-leaved-fullscreen" [::window.events/set-fullscreen false]]
                ["window-minimized" [::window.events/set-minimized true]]
                ["window-loaded" [::add-listeners]]]
               (mapv #(vector ::effects/ipc-on %)))})))
