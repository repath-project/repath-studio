(ns electron.preload
  (:require
   #_["@sentry/electron" :as Sentry]
   ["electron" :refer [contextBridge ipcRenderer]]
   ["font-scanner" :as fontManager]
   ["opentype.js" :as opentype]
   ["os" :as os]
   [config]))

(defn text->path
  "https://github.com/opentypejs/opentype.js#loading-a-font-synchronously-nodejs"
  [text {:keys [font-url x y font-size]}]
  (let [font (.loadSync opentype font-url)
        path (.getPath font text x y font-size)]
    (.toPathData path)))

(defonce api
  {;; Strip event as it includes `sender`
   ;; https://www.electronjs.org/docs/latest/api/ipc-renderer#ipcrendereronchannel-listener
   :on (fn [channel f] (.on ipcRenderer channel (fn [_e args] (f args))))
   :send (fn [channel args] (.send ipcRenderer channel args))
   :invoke (fn [channel args] (.invoke ipcRenderer channel args))
   :platform (.platform os)
   :findFonts (fn [descriptor] (.findFontsSync fontManager descriptor))
   :textToPath (fn [s options]
                 (text->path s (js->clj options :keywordize-keys true)))})

(defn ^:export init []
  ;; https://docs.sentry.io/platforms/javascript/guides/electron/#configuring-the-client
  #_(.init Sentry (clj->js config/sentry-options))
  ;; Expose protected methods that allow the renderer process to use the
  ;; ipcRenderer without exposing the entire object
  ;; https://www.electronjs.org/docs/api/context-bridge
  (.exposeInMainWorld contextBridge "api" (clj->js api)))
