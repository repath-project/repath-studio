(ns renderer.snap.events
  (:require
   [clojure.set :as set]
   [re-frame.core :as rf]
   [renderer.app.effects :refer [persist]]
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

(def auto-rebuild-tree
  "Updates the kdtree when the selection changes, by adding/removing points."
  (rf/->interceptor
   :id ::auto-rebuild-tree
   :before (fn [context]
             (let [db (rf/get-coeffect context :db)]
               (cond-> context
                 (:active-document db)
                 (rf/assoc-coeffect :non-selected-ids (element.h/non-selected-ids db)))))
   :after (fn [context]
            (let [db (rf/get-effect context :db)]
              (if (:active-document db)
                (let [previous-non-selected-ids (rf/get-coeffect context :non-selected-ids)
                      non-selected-ids (element.h/non-selected-ids db)]
                  (cond-> context
                    (not= non-selected-ids previous-non-selected-ids)
                    (rf/assoc-effect
                     :db
                     (-> db
                         (h/insert-to-tree (set/difference non-selected-ids previous-non-selected-ids))
                         (h/delete-from-tree (set/difference previous-non-selected-ids non-selected-ids))))))
                context)))))

(rf/reg-global-interceptor auto-rebuild-tree)
