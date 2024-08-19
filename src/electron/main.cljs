(ns electron.main
  (:require
   #_["@sentry/electron/main" :as sentry-electron-main]
   ["@webref/css" :as css]
   ["electron" :refer [app shell ipcMain BrowserWindow]]
   ["electron-extension-installer" :refer [REACT_DEVELOPER_TOOLS installExtension]]
   ["electron-log/main" :as log]
   ["electron-reloader"]
   ["electron-updater" :as updater]
   ["electron-window-state" :as window-state-keeper]
   ["font-scanner" :as fontManager]
   ["os" :as os]
   ["path" :as path]
   [config :as config]
   [electron.file :as file]))

(defonce main-window (atom nil))
(defonce loading-window (atom nil))

(defn add-extension
  [extension]
  (-> (installExtension
       extension
       #js {:loadExtensionOptions {:allowFileAccess true}})
      (.then (fn [extension] (js/console.log "Added Extension: " extension)))
      (.catch (fn [err] (js/console.log "An error occurred: " err)))))

(defn send-to-renderer!
  ([channel]
   (send-to-renderer! channel nil))
  ([channel data]
   (.send (.-webContents ^js @main-window) channel (clj->js data))))

(def allowed-urls
  #{"repath.studio"
    "github.com"
    "developer.mozilla.org"
    "svgwg.org"
    "fxtf.org"
    "drafts.fxtf.org"
    "csswg.org"
    "drafts.csswg.org"})

(defn allowed-url?
  [url]
  (contains? allowed-urls (.-host url)))

(defn open-external!
  [url]
  (let [url-parsed (js/URL. url)]
    (when (and (= (.-protocol url-parsed) "https:")
               (allowed-url? url-parsed))
      (.openExternal shell url-parsed.href))))

(defn register-ipc-on-events!
  []
  (doseq
   [[e f]
    [["open-remote-url" #(open-external! %)]
     ["open-directory" #(.showItemInFolder shell %)]
     ["window-minimize" #(.minimize ^js @main-window)]
     ["window-toggle-fullscreen" #(.setFullScreen ^js @main-window (not (.isFullScreen ^js @main-window)))]
     ["window-toggle-maximized" #(if (.isMaximized ^js @main-window)
                                   (.unmaximize ^js @main-window)
                                   (.maximize ^js @main-window))]]]
    (.on ipcMain e #(f %2))))

(defn register-ipc-handle-events!
  []
  (doseq
   [[e f]
    [["open-documents" #(file/open! @main-window %)]
     ["save-document" #(file/save! @main-window %)]
     ["save-document-as" #(file/save-as! @main-window %)]
     ["export" #(file/export! @main-window %)]
     ["print" #(file/print! %)]
     ["load-webref" #(.listAll css)]
     ["load-system-fonts" #(.getAvailableFonts fontManager)]]]
    (.handle ipcMain e #(f %2))))

(defn register-window-events!
  []
  (doseq
   [[window-event f]
    [["maximize" #(send-to-renderer! "window-maximized")]
     ["unmaximize" #(send-to-renderer! "window-unmaximized")]
     ["enter-full-screen" #(send-to-renderer! "window-entered-fullscreen")]
     ["leave-full-screen" #(send-to-renderer! "window-leaved-fullscreen")]
     ["minimize" #(send-to-renderer! "window-minimized")]
     ["restore" #(send-to-renderer! "window-restored")]
     ;; Event "resized" is more suitable, but it's not supported on linux
     ;; https://www.electronjs.org/docs/latest/api/browser-window#event-resized-macos-windows
     ["resize" #(send-to-renderer! (if (.isMaximized ^js @main-window)
                                     "window-maximized"
                                     "window-unmaximized"))]]]
    (.on ^js @main-window window-event f)))

(defn register-web-contents-events!
  []
  (doseq
   [[web-contents-event f]
    [["will-frame-navigate" #(.preventDefault %)] ;; Prevent navigation
     ["closed" #(reset! main-window nil)]]]
    (.on (.-webContents ^js @main-window) web-contents-event f)))

(defn init-main-window!
  []
  (let [win-state (window-state-keeper #js {:defaultWidth 1920
                                            :defaultHeight 1080})]
    (reset! main-window
            (BrowserWindow.
             #js {:x (.-x win-state)
                  :y (.-y win-state)
                  :width (.-width win-state)
                  :height (.-height win-state)
                  :backgroundColor "#313131"
                  :titleBarStyle (when (= (.platform os) "darwin") "hidden")
                  :trafficLightPosition #js {:x 8 :y 10}
                  :icon (.join path js/__dirname "/public/img/icon.png")
                  :frame false
                  :show false
                  :webPreferences
                  #js {:sandbox false
                       :preload (.join path js/__dirname "preload.js")}}))

    (.once ^js @main-window "ready-to-show"
           (fn []
             (.show ^js @main-window)
             (.manage win-state ^js @main-window)
             (send-to-renderer! (if (.isMaximized ^js @main-window)
                                  "window-maximized"
                                  "window-unmaximized"))
             (send-to-renderer! (if (.isFullScreen ^js @main-window)
                                  "window-entered-fullscreen"
                                  "window-leaved-fullscreen"))
             (.hide ^js @loading-window)
             (.close ^js @loading-window)))

    (.loadURL ^js @main-window (if config/debug?
                                 "http://localhost:8080"
                                 (.join path "file://" js/__dirname "/public/index.html")))

    (when config/debug?
      (add-extension REACT_DEVELOPER_TOOLS)
      #_(.openDevTools (.-webContents ^js @main-window)))

    (register-web-contents-events!)
    (register-ipc-on-events!)
    (register-ipc-handle-events!)
    (register-window-events!)

    (when (not= (.platform os) "linux")
      (.checkForUpdatesAndNotify updater))))

(defn init-loading-window! []
  (set! (.-allowRendererProcessReuse app) false)
  (reset! loading-window
          (BrowserWindow.
           #js {:width 720
                :height 576
                :backgroundColor "#313131"
                :icon (.join path js/__dirname "/public/img/icon.png")
                :show false
                :frame false}))
  (.once ^js @loading-window "show" init-main-window!)
  (.loadURL ^js @loading-window (.join path "file://" js/__dirname "/public/loading.html"))
  (.once ^js (.-webContents @loading-window) "did-finish-load" #(.show ^js @loading-window)))

(defn ^:export init []
  #_(sentry-electron-main/init (clj->js config/sentry-options))
  (.initialize log)
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on app "ready" init-loading-window!))
