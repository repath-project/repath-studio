(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [renderer.tree.effects :as fx]))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ id]]
   {::fx/focus [id :up]}))

(rf/reg-event-fx
 ::focus-down
 (fn [_ [_ id]]
   {::fx/focus [id :down]}))

(rf/reg-event-fx
 ::select-range
 (fn [_ [_ last-focused-id id]]
   {::fx/select-range [last-focused-id id]}))
