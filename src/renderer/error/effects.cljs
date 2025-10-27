(ns renderer.error.effects
  (:require
   ["@sentry/capacitor" :as sentry-capacitor]
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   [re-frame.core :as rf]))

(rf/reg-fx
 ::init-reporting
 (fn [[platform config]]
   (cond
     (= platform "web")
     (sentry-react/init config)

     (contains? #{"darwin" "win32" "linux"} platform)
     (sentry-electron-renderer/init config sentry-react/init)

     (contains? #{"ios" "android"} platform)
     (sentry-capacitor/init config sentry-react/init))))
