(ns renderer.error.effects
  (:require
   ["@sentry/electron/renderer" :as sentry-electron-renderer]
   ["@sentry/react" :as sentry-react]
   [re-frame.core :as rf]))

(rf/reg-fx
 ::init-reporting
 (fn [config]
   (if (.-api js/window)
     (sentry-electron-renderer/init config sentry-react/init)
     (sentry-react/init config))))
