(ns renderer.document.effects
  (:require
   [de-dupe.core :as dd]
   [re-frame.core :as rf]))

(rf/reg-event-fx
 :document/open
 (fn [_ [_]]
   {:send-to-main {:action "openDocument"}}))

(rf/reg-event-fx
 :document/save
 (fn [{:keys [db]} [_]]
   (let [document (get-in db [:documents (:active-document db)])
         duped (assoc document :history (dd/de-dupe (:history document)))]
     {:send-to-main {:action "saveDocument" :data (pr-str duped)}})))

(rf/reg-event-fx
 :document/save-as
 (fn [{:keys [db]} [_]]
   db))

(rf/reg-event-fx
 :document/save-all
 (fn [{:keys [db]} [_]]
   db))
