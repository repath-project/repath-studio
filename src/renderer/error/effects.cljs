(ns renderer.error.effects
  (:require
   ["@sentry/capacitor" :as sentry-capacitor]
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   [re-frame.core :as rf]
   [renderer.app.handlers :as app.handlers]))

(rf/reg-fx
 ::init-reporting
 (fn [[platform config]]
   (cond-> config
     (app.handlers/desktop? platform)
     (sentry-electron-renderer/init sentry-react/init)

     (app.handlers/mobile? platform)
     (sentry-capacitor/init sentry-react/init)

     (app.handlers/web? platform)
     (sentry-react/init))))
