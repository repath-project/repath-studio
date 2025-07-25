(ns renderer.theme.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.effects :as-alias effects]
   [renderer.theme.db :as theme.db]
   [renderer.theme.effects :as-alias theme.effects]
   [renderer.theme.handlers :as theme.handlers]))

(rf/reg-event-fx
 ::set-document-attr
 [(rf/inject-cofx ::theme.effects/native-mode)]
 (fn [{:keys [db native-mode]} _]
   (let [mode (-> db :theme :mode)
         mode (if (= mode :system) native-mode mode)]
     {:db (theme.handlers/set-native-mode db native-mode)
      ::effects/set-document-attr ["data-theme" (name mode)]})))

(rf/reg-event-fx
 ::cycle-mode
 [persist]
 (fn [{:keys [db]} [_]]
   (let [index (.indexOf theme.db/modes (-> db :theme :mode))
         mode (or (get theme.db/modes (inc index))
                  (first theme.db/modes))]
     {:db (theme.handlers/set-mode db mode)
      :dispatch [::set-document-attr]})))
