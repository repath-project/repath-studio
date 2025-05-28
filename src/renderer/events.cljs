(ns renderer.events
  (:require
   [re-frame.core :as rf]
   [renderer.effects :as-alias effects]
   [renderer.utils.system :as utils.system]))

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

(rf/reg-event-fx
 ::open-remote-url
 (fn [_ [_ url]]
   (if utils.system/electron?
     {::effects/ipc-send ["open-remote-url" url]}
     {::effects/open-remote-url url})))
