(ns electron.main
  (:require
   #_["@sentry/electron/main" :as sentry-electron-main]
   ["@webref/css" :as css]
   ["electron-extension-installer" :refer [REACT_DEVELOPER_TOOLS installExtension]]
   ["electron-log/main" :as log]
   ["electron-reloader"]
   #_["electron-updater" :as updater]
   ["electron-window-state" :as window-state-keeper]
   ["electron" :refer [app shell ipcMain BrowserWindow clipboard nativeTheme]]
   ["font-scanner" :as fontManager]
   ["os" :as os]
   ["path" :as path]
   [config]
   [electron.file :as file]
   [promesa.core :as p]))

(defonce main-window (atom nil))
(defonce loading-window (atom nil))

(defn add-extension
  [extension]
  (-> (installExtension
       extension
       #js {:loadExtensionOptions {:allowFileAccess true}})
      (.then (fn [name] (js/console.log "Added Extension: " name)))
      (.catch (fn [err] (js/console.log "An error occurred: " err)))))

(defn send-to-renderer!
  ([action]
   (send-to-renderer! action nil))
  ([action data]
   (-> (.-webContents ^js @main-window)
       (.send "fromMain" (clj->js {:action action
                                   :data data})))))

(defonce allowed-urls
  #{"repath.studio"
    "github.com"
    "developer.mozilla.org"
    "svgwg.org"})

(defn allowed-url?
  [url]
  (contains? allowed-urls (.-host url)))

(defn open-external!
  [url]
  (let [url-parsed (js/URL. url)]
    (when (and (= (.-protocol url-parsed) "https:") (allowed-url? url-parsed))
      (.openExternal shell url-parsed.href))))

(defn to-main-api
  [args]
  (case (.-action args)
    "windowMinimize" (.minimize ^js @main-window)
    "windowToggleMaximized" (if (.isMaximized ^js @main-window) (.unmaximize ^js @main-window) (.maximize ^js @main-window))
    "windowToggleFullscreen" (.setFullScreen ^js @main-window (not (.isFullScreen ^js @main-window)))
    "setThemeMode" (set! (.. nativeTheme -themeSource) (.-data args))
    "openRemoteUrl" (open-external! (.-data args))
    ;; https://www.electronjs.org/docs/api/clipboard#clipboardwritedata-type
    "writeToClipboard" (.write clipboard (.-data args))
    "openDirectory" (.showItemInFolder shell (.-data args))
    "openDocument" (p/let [documents (file/open! @main-window (.-data args))] (doseq [document documents] (send-to-renderer! "fileLoaded" document)))
    "saveDocument" (p/let [document (file/save! @main-window (.-data args))] (send-to-renderer! "fileSaved" document))
    "saveDocumentAs" (p/let [document (file/save-as! @main-window (.-data args))] (send-to-renderer! "fileSaved" document))
    "export" (file/export! @main-window (.-data args))))

(defn register-window-events!
  []
  (doseq
   [[window-event action]
    [;; Event "resized" is more suitable, but it's not supported on linux
     ;; https://www.electronjs.org/docs/latest/api/browser-window#event-resized-macos-windows
     ["resize" #(if (.isMaximized ^js @main-window) "windowMaximized" "windowUnmaximized")]
     ["maximize" "windowMaximized"]
     ["unmaximize" "windowUnmaximized"]
     ["enter-full-screen" "windowEnteredFullscreen"]
     ["leave-full-screen" "windowLeavedFullscreen"]
     ["minimize" "windowMinimized"]
     ["restore" "windowRestored"]]]
    (.on ^js @main-window window-event #(send-to-renderer! action))))

(defn load-system-fonts!
  "https://github.com/axosoft/font-scanner#getavailablefonts"
  []
  (let [fonts (.getAvailableFontsSync fontManager)]
    (send-to-renderer! "fontsLoaded" fonts)))

(defn load-webref!
  []
  (p/let [files (.listAll css)]
    (send-to-renderer! "webrefLoaded" files)))

(defn register-web-contents-events!
  []
  (doseq
   [[web-contents-event f]
    [["will-frame-navigate" #(.preventDefault %)] ;; Prevent navigation
     ["closed" #(reset! main-window nil)]]]
    (.on (.-webContents ^js @main-window) web-contents-event f))
  ;; Forward popups
  (.setWindowOpenHandler (.-webContents ^js @main-window) (fn [handler]
                                                            (open-external! (.-url handler))
                                                            #js {:action "deny"})))

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
                                  "windowMaximized"
                                  "windowUnmaximized"))
             (send-to-renderer! (if (.isFullScreen ^js @main-window)
                                  "windowEnteredFullscreen"
                                  "windowLeavedFullscreen"))
             (.hide ^js @loading-window)
             (.close ^js @loading-window)))

    (.on ^js @main-window "ready-to-show"
         (fn []
           (load-system-fonts!)
           (load-webref!)))

    (.loadURL ^js @main-window (if config/debug?
                                 "http://localhost:8080"
                                 (.join path "file://" js/__dirname "/public/index.html")))

    (when config/debug?
      (add-extension REACT_DEVELOPER_TOOLS)
      #_(.openDevTools (.-webContents ^js @main-window)))

    (register-web-contents-events!)
    (.on ipcMain "toMain" #(to-main-api %2))
    (register-window-events!)

    #_(.checkForUpdatesAndNotify updater)))

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
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                  (.quit app)))
  (.on app "ready" init-loading-window!))
