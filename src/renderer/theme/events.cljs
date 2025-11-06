(ns renderer.theme.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.app.handlers :as app.handlers]
   [renderer.effects :as-alias effects]
   [renderer.theme.db :as theme.db]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.theme.handlers :as theme.handlers]))

(rf/reg-event-fx
 ::set-native-mode
 [(rf/inject-cofx ::theme.effects/native-mode)]
 (fn [{:keys [db native-mode]} _]
   {:db (theme.handlers/set-native-mode db native-mode)
    :dispatch-n [[::update-data-theme]
                 [::update-meta-color]
                 [::update-status-bar]]}))

(rf/reg-event-fx
 ::update-data-theme
 (fn [{:keys [db]} _]
   (let [mode (theme.handlers/computed-mode db)]
     {::effects/set-document-attr ["data-theme" (when mode (name mode))]})))

(rf/reg-event-fx
 ::update-status-bar
 (fn [{:keys [db]} _]
   (when (app.handlers/mobile? (:platform db))
     (let [mode (theme.handlers/computed-mode db)]
       {::theme.effects/set-status-bar-style mode}))))

(rf/reg-event-fx
 ::update-meta-color
 [(rf/inject-cofx ::theme.effects/theme-color)]
 (fn [{:keys [theme-color]} _]
   {::effects/set-meta ["theme-color" theme-color]}))

(rf/reg-event-fx
 ::set-mode
 [persist]
 (fn [{:keys [db]} [_ mode]]
   {:db (theme.handlers/set-mode db mode)
    :dispatch-n [[::update-data-theme]
                 [::update-meta-color]
                 [::update-status-bar]]}))

(rf/reg-event-fx
 ::cycle-mode
 [persist]
 (fn [{:keys [db]} [_]]
   (let [index (.indexOf theme.db/modes (-> db :theme :mode))
         mode (or (get theme.db/modes (inc index))
                  (first theme.db/modes))]
     {:dispatch [::set-mode mode]})))
