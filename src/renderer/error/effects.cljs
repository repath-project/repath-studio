(ns renderer.error.effects
  (:require
   ["@sentry/capacitor" :as sentry-capacitor]
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   [re-frame.core :as rf]))

(rf/reg-fx
 ::init-reporting
 (fn [[platform config]]
   (condp contains? platform
     #{"darwin" "win32" "linux"}
     (sentry-electron-renderer/init config sentry-react/init)

     #{"ios" "android"}
     (sentry-capacitor/init config sentry-react/init)

     (sentry-react/init config))))
