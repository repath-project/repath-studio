(ns repath.studio.documents.effects
  (:require
   [re-frame.core :as rf]
   [de-dupe.core :as dd]))

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