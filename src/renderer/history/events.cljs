(ns renderer.history.events
  (:require [re-frame.core :as rf]
            [renderer.tools.base :as tools]
            [renderer.history.handlers :as handlers]
            [renderer.element.handlers :as elements]))

(rf/reg-event-db
 :history/undo
 (fn [db [_ n]]
   (handlers/undo db n)))

(rf/reg-event-db
 :history/redo
 (fn [db [_ n]]
   (handlers/redo db n)))

(rf/reg-event-db
 :history/cancel
 (fn [db _]
   (cond-> db
     (and (= (:tool db) :select) (not (:mouse-offset db))) (-> (elements/deselect-all)
                                                               (handlers/finalize "Deselect all"))
     (not (:mouse-offset db)) (-> (tools/set-tool :select)
                                  (assoc :state :default))
     (:mouse-offset db) (dissoc :mouse-offset)
     :always (-> (elements/clear-temp)
                 (handlers/swap)))))