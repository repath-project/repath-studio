(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]
   [renderer.events :as-alias events]))

(rf/reg-event-fx
 ::select-item
 (fn [_ [_ dispatch]]
   {:dispatch-n [dispatch
                 [::events/focus nil]]}))
