(ns preload
  (:require
   #_["@sentry/electron" :as Sentry]
   ["@webref/css" :as css]
   ["electron" :refer [contextBridge ipcRenderer]]
   ["font-scanner" :as fontManager]
   ["mdn-data" :as mdn] ;; deprecating in favor of w3c/webref
   ["opentype.js" :as opentype]
   [config]))

(defn text->path
  "https://github.com/opentypejs/opentype.js#loading-a-font-synchronously-nodejs"
  [text {:keys [font-url x y font-size]}]
  (js/console.log font-url x y font-size)
  (let [font (.loadSync opentype font-url)
        path (.getPath font text x y font-size)]
    (.toPathData path)))

(defonce api
  {:send (fn [channel data] (.send ipcRenderer channel data))
   :receive (fn [channel f]
              ;; Strip event (_) as it includes `sender`
              (.on ipcRenderer channel (fn [_ args] (f args))))
   :mdn mdn
   :webrefCss css
   ;; https://github.com/axosoft/font-scanner#getavailablefonts
   :systemFonts (.getAvailableFontsSync fontManager)
   :findFonts (fn [descriptor] (.findFontsSync fontManager descriptor))
   :textToPath (fn [text options]
                 (text->path text (js->clj options :keywordize-keys true)))})

(defn ^:export init []
  ;; https://docs.sentry.io/platforms/javascript/guides/electron/#configuring-the-client
  #_(.init Sentry (clj->js config/sentry-options))
  ;; Expose protected methods that allow the renderer process to use the 
  ;; ipcRenderer without exposing the entire object
  ;; https://www.electronjs.org/docs/api/context-bridge
  (.exposeInMainWorld contextBridge "api" (clj->js api)))
