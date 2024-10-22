(ns renderer.menubar.events
  (:require
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]))

(rf/reg-event-fx
 ::select-item
 (fn [_ [_ dispatch]]
   {:dispatch-n [dispatch
                 [::app.e/focus nil]]}))
