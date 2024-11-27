(ns renderer.snap.events
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.element.handlers :as element.h]
   [renderer.snap.handlers :as h]))

(rf/reg-event-db
 ::toggle
 [persist]
 (fn [db [_]]
   (-> (update-in db [:snap :active] not)
       (h/rebuild-tree))))

(rf/reg-event-db
 ::toggle-option
 [persist]
 (fn [db [_ option]]
   (-> (h/toggle-option db option)
       (h/rebuild-tree))))

(rf/reg-global-interceptor
 (rf/->interceptor
  :id ::auto-update-tree
  :after (fn [context]
           (let [db (rf/get-effect context :db)]
             (if (:active-document db)
               (let [non-selected-ids (element.h/non-selected-ids db)
                     prev-non-selected-ids (let [db (rf/get-coeffect context :db)]
                                             (when (:active-document db)
                                               (element.h/non-selected-ids db)))]
                 (cond-> context
                   (not= non-selected-ids prev-non-selected-ids)
                   (rf/assoc-effect
                    :db
                    (-> db
                        (h/insert-to-tree (set/difference non-selected-ids prev-non-selected-ids))
                        (h/delete-from-tree (set/difference prev-non-selected-ids non-selected-ids))))))
               context)))))
