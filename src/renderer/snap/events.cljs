(ns renderer.snap.events
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.element.handlers :as element.handlers]
   [renderer.snap.handlers :as snap.handlers]))

(rf/reg-event-db
 ::toggle
 [persist]
 (fn [db [_]]
   (-> (update-in db [:snap :active] not)
       (snap.handlers/rebuild-tree))))

(rf/reg-event-db
 ::toggle-option
 [persist]
 (fn [db [_ option]]
   (-> (snap.handlers/toggle-option db option)
       (snap.handlers/rebuild-tree))))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::auto-update-tree
  :after (fn [context]
           (let [db (rf/get-effect context :db)]
             (if (:active-document db)
               (let [non-selected-ids (element.handlers/non-selected-ids db)
                     prev-non-selected-ids (let [db (rf/get-coeffect context :db)]
                                             (when (:active-document db)
                                               (element.handlers/non-selected-ids db)))]
                 (cond-> context
                   (not= non-selected-ids prev-non-selected-ids)
                   (rf/assoc-effect
                    :db
                    (-> db
                        (snap.handlers/insert-to-tree (set/difference non-selected-ids prev-non-selected-ids))
                        (snap.handlers/delete-from-tree (set/difference prev-non-selected-ids non-selected-ids))))))
               context)))))
