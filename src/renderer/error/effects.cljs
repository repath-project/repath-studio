(ns renderer.error.effects
  (:require
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   [re-frame.core :as rf]))

(rf/reg-fx
 ::init-reporting
 (fn [[platform config]]
   (if (= platform "web")
     (sentry-react/init config)
     (sentry-electron-renderer/init config sentry-react/init))))
