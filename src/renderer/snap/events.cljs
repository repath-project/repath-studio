(ns renderer.snap.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.effects :refer [persist]]
   [renderer.element.handlers :as element.h]
   [renderer.snap.handlers :as h]))

(rf/reg-event-db
 ::toggle
 [persist]
 (fn [db [_]]
   (-> (update-in db [:snap :active] not)
       (h/update-tree))))

(rf/reg-event-db
 ::toggle-option
 [persist]
 (fn [db [_ option]]
   (-> (h/toggle-option db option)
       (h/update-tree))))

(def auto-update-tree
  (rf/->interceptor
   :id ::auto-update-tree
   :before (fn [context]
             (let [db (rf/get-coeffect context :db)]
               (rf/assoc-coeffect context :non-selected-ids (element.h/non-selected-ids db))))
   :after (fn [context]
            (let [db (rf/get-effect context :db)]
              (cond-> context
                (and (:active-document db)
                     (not= (element.h/non-selected-ids db)
                           (rf/get-coeffect context :non-selected-ids)))
                (rf/assoc-effect :db (h/update-tree db)))))))

(rf/reg-global-interceptor auto-update-tree)
