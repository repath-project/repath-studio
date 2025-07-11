(ns core-test
  (:require
   [malli.instrument :as m.instrument]
   [re-frame.core :as rf]
   [re-frame.subs :as rf.subs]
   [renderer.app.events :as app.events]
   [renderer.app.subs]
   [renderer.dialog.events]
   [renderer.document.events]
   [renderer.document.subs]
   [renderer.element.effects]
   [renderer.element.events]
   [renderer.element.impl.core]
   [renderer.element.subs]
   [renderer.frame.events]
   [renderer.frame.subs]
   [renderer.history.events]
   [renderer.history.subs]
   [renderer.notification.events]
   [renderer.notification.subs]
   [renderer.ruler.events]
   [renderer.snap.events]
   [renderer.theme.events]
   [renderer.theme.subs]
   [renderer.timeline.events]
   [renderer.tool.events]
   [renderer.tool.impl.core]
   [renderer.tool.subs]
   [renderer.tree.events]
   [renderer.window.events]
   [renderer.window.subs]
   [renderer.worker.events]))

(set! rf.subs/warn-when-not-reactive (constantly nil))

(defn ^:dev/after-load instrument! []
  (m.instrument/instrument!)
  (rf/reg-global-interceptor app.events/schema-validator))

(instrument!)
