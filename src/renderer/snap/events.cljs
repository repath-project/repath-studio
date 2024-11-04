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
               (rf/assoc-coeffect context :non-selected-ids (element.h/non-selected-ids db))))
   :after (fn [context]
            (let [db (rf/get-effect context :db)
                  previous-non-selected-ids (rf/get-coeffect context :non-selected-ids)
                  non-selected-ids (element.h/non-selected-ids db)]
              (cond
                (and (:active-document db)
                     (not= non-selected-ids previous-non-selected-ids))
                (rf/assoc-effect
                 context
                 :db
                 (-> db
                     (h/insert-to-tree (set/difference non-selected-ids previous-non-selected-ids))
                     (h/delete-from-tree (set/difference previous-non-selected-ids non-selected-ids))))

                :else
                context)))))

(rf/reg-global-interceptor auto-rebuild-tree)
