(ns renderer.events
  (:require
   [re-frame.core :as rf]
   [renderer.effects :as-alias effects]))

(rf/reg-event-fx
 ::focus
 (fn [_ [_ id]]
   {::effects/focus id}))

(rf/reg-event-fx
 ::scroll-into-view
 (fn [_ [_ el]]
   {::effects/scroll-into-view el}))

(rf/reg-event-fx
 ::scroll-to-bottom
 (fn [_ [_ el]]
   {::effects/scroll-to-bottom el}))

(rf/reg-event-fx
 ::file-open
 (fn [_ [_ options]]
   {::effects/file-open options}))
