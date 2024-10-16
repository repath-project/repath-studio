(ns renderer.app.events
  (:require
   [config :as config]
   [malli.error :as me]
   [re-frame.core :as rf]
   [renderer.app.db :as db]
   [renderer.app.effects :as fx :refer [persist]]
   [renderer.app.handlers :as h]
   [renderer.frame.handlers :as frame.h]
   [renderer.history.handlers :as history.h :refer [finalize]]
   [renderer.notification.events :as-alias notification.e]
   [renderer.notification.handlers :as notification.h]
   [renderer.notification.views :as notification.v]
   [renderer.utils.i18n :as i18n]
   [renderer.utils.system :as system]
   [renderer.window.effects :as-alias window.fx]))

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default))

(rf/reg-event-fx
 ::load-local-db
 [(rf/inject-cofx :store)]
 (fn [{:keys [db store]} _]
   (let [app-db (merge db store)]
     (if (db/valid? app-db)
       {:db app-db}
       {::fx/local-storage-clear nil
        :db (cond-> db
              config/debug?
              (notification.h/add (notification.v/spec-failed
                                   "Invalid local configuration"
                                   (-> app-db db/explain me/humanize str))))}))))

(rf/reg-event-fx
 ::persist
 (fn [{:keys [db]} _]
   {::fx/persist db}))

(rf/reg-event-db
 ::set-system-fonts
 (fn [db [_ fonts]]
   (assoc db :system-fonts fonts)))

(rf/reg-event-db
 ::set-webref-css
 (fn [db [_ webref-css]]
   (assoc db :webref-css webref-css)))

(rf/reg-event-fx
 ::set-tool
 (fn [{:keys [db]} [_ tool]]
   {:db (h/set-tool db tool)
    ::fx/focus nil}))

(rf/reg-event-db
 ::set-lang
 (fn [db [_ lang]]
   (cond-> db
     (i18n/lang? lang)
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
 ::set-backdrop
 (fn [db [_ visible]]
   (assoc db :backdrop visible)))

(rf/reg-event-db
 ::toggle-grid
 persist
 (fn [db [_]]
   (update db :grid not)))

(rf/reg-event-db
 ::toggle-panel
 [persist
  (rf/path :panels)]
 (fn [db [_ k]]
   (update-in db [k :visible] not)))

(rf/reg-event-fx
 ::pointer-event
 [(rf/inject-cofx ::fx/now)
  (finalize nil)]
 (fn [{:keys [db now]} [_ e]]
   {:db (h/pointer-handler db e now)}))

(rf/reg-event-db
 ::wheel-event
 (fn [db [_ e]]
   (h/wheel-handler db e)))

(rf/reg-event-fx
 ::drag-event
 (fn [{:keys [db]} [_ {:keys [data-transfer pointer-pos] :as e}]]
   (when (= (:type e) "drop")
     {::fx/data-transfer [(frame.h/adjust-pointer-pos db pointer-pos) data-transfer]})))

(rf/reg-event-db
 ::keyboard-event
 (fn [db [_ e]]
   (h/key-handler db e)))

(rf/reg-event-fx
 ::focus
 (fn [_ [_ id]]
   {::fx/focus id}))

(defn ->font-map
  [^js/FontData font-data]
  (into {} [[:postscriptName (.-postscriptName font-data)]
            [:fullName (.-fullName font-data)]
            [:family (.-family font-data)]
            [:style (.-style font-data)]]))

(rf/reg-event-fx
 ::load-system-fonts
 (fn [_ [_ file-path]]
   (if system/electron?
     {::window.fx/ipc-invoke {:channel "load-system-fonts"
                              :data file-path
                              :on-resolution ::set-system-fonts
                              :formatter #(js->clj % :keywordize-keys true)}}
     {::fx/query-local-fonts {:on-resolution ::set-system-fonts
                              :formatter #(mapv ->font-map %)}})))

(rf/reg-event-fx
 ::load-webref
 (fn [_ [_ file-path]]
   (when system/electron?
     {::window.fx/ipc-invoke {:channel "load-webref"
                              :data file-path
                              :on-resolution ::set-webref-css
                              :formatter #(js->clj % :keywordize-keys true)}})))
