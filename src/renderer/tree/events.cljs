(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [renderer.tree.effects :as fx]))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ k]]
   {::fx/focus [k :up]}))

(rf/reg-event-fx
 ::focus-down
 (fn [_ [_ k]]
   {::fx/focus [k :down]}))
