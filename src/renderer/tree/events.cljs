(ns renderer.tree.events
  (:require
   [re-frame.core :as rf]
   [renderer.tree.effects :as-alias tree.effects]))

(rf/reg-event-fx
 ::focus-up
 (fn [_ [_ id]]
   {::tree.effects/focus-next [id :up]}))

(rf/reg-event-fx
 ::focus-down
 (fn [_ [_ id]]
   {::tree.effects/focus-next [id :down]}))

(rf/reg-event-fx
 ::select-range
 (fn [_ [_ last-focused-id id]]
   {::tree.effects/select-range [last-focused-id id]}))
