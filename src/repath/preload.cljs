(ns repath.preload
  (:require ["electron" :refer [contextBridge ipcRenderer]]
            ["@mdn/browser-compat-data" :as bcd]
            ["mdn-data" :as mdn]
            ["font-list" :as fontList]
            ["opentype.js" :as opentype]
            ["@sentry/electron" :as Sentry]
            [repath.config :as config]))

(defn init-sentry
  "SEE https://docs.sentry.io/platforms/javascript/guides/electron/"
  []
  (.init Sentry (clj->js config/sentry-options)))

(defn text-to-path
  "SEE https://github.com/opentypejs/opentype.js#loading-a-font-synchronously-nodejs"
  [font-url text x y font-size]
  (let [font (.loadSync opentype font-url)
        path (.getPath font text x y font-size)]
    (.toPathData path)))

(defonce api
  {:send (fn [channel data] (.send ipcRenderer channel data))
   :receive (fn [channel func] (.on ipcRenderer channel (fn [_ args] (func args)))) ; Strip event (_) as it includes `sender`
   :mdn mdn
   :bcd bcd
   :fontList fontList
   :textToPath text-to-path 
   :initSentry init-sentry})

(defn main []
  #_(init-sentry)
  ;; Expose protected methods that allow the renderer process to use the ipcRenderer without exposing the entire object
  ;; SEE https://www.electronjs.org/docs/api/context-bridge
  (.exposeInMainWorld contextBridge "api" (clj->js api)))

