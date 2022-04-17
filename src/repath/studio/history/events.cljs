(ns repath.studio.history.events
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.history.handlers :as h]
            [repath.studio.elements.handlers :as elements]))

(rf/reg-event-db
 :history/undo
 (fn [db [_ n]]
   (h/undo db n)))

(rf/reg-event-db
 :history/redo
 (fn [db [_ n]]
    (h/redo db n)))

(rf/reg-event-db
 :history/cancel
 (fn [db _]
   (cond-> db
     (and (= (:tool db) :select) (not (:mouse-offset db))) (-> (elements/deselect-all)
                                                               (h/finalize "Deselect all"))
     (not (:mouse-offset db)) (-> (tools/set-tool :select)
                                  (assoc :state :default))
     (:mouse-offset db) (dissoc :mouse-offset)
     :always (-> (elements/clear-temp)
                 (h/swap)))))