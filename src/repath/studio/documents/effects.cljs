(ns repath.studio.documents.effects
  (:require
   [re-frame.core :as rf]
   [de-dupe.core :as dd]))

(rf/reg-event-fx
 :document/open
 (fn [_ [_]]
   {::open nil}))

(rf/reg-event-fx
 :document/save
 (fn [{:keys [db]} [_]]
   (let [document (get-in db [:documents (:active-document db)])
         duped (assoc document :history (dd/de-dupe (:history document)))]
     {::save duped})))

(rf/reg-fx
 ::open
 (fn [_]
   (.then (js/window.api.send "toMain" #js {:action "openDocument"}))))

(rf/reg-fx
 ::save
 (fn [data]
   (.then (js/window.api.send "toMain" #js {:action "saveDocument" :data (pr-str data)}))))
