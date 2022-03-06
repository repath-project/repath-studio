(ns repath.main
  (:require ["electron" :refer [app shell ipcMain BrowserWindow clipboard nativeTheme]]
            ["path" :as path]
            ["electron-updater" :as updater]
            ["@sentry/electron" :as Sentry]    
            [repath.config :as config]
            [repath.file :as file]))

(def main-window (atom nil))
(def loading-window (atom nil))

(defonce loading-window-options {:width 644
                                 :height 368
                                 :backgroundColor "#313131"
                                 :icon (str js/__dirname "/public/img/icon.png")
                                 :show false
                                 :frame false})

(defonce main-window-options {:width 1280
                              :height 920
                              :backgroundColor "#313131"
                              :icon (str js/__dirname "/public/img/icon.png")
                              :frame false
                              :show false
                              :webPreferences {:devTools config/debug?
                                               :preload (.resolve path (str js/__dirname "/preload.js"))}})

(set! (.. nativeTheme -themeSource) "dark")

(defn send-frames-to-renderer
  "Sends bitmap frames to renderer for color picking purposes"
  [image dirtyRect]
  (.send  (.-webContents ^js @main-window) "fromMain" #js {:action "windowPainted" 
                                                           :data #js {:bitmap (.toBitmap ^js image)
                                                                      :size (.getSize ^js image)}}))

(defn to-main-api
  [args]
  (case (.-action args)
    "windowMinimize" (.minimize ^js @main-window)
    "windowToggleMaximized" (if (.isMaximized ^js @main-window) (.unmaximize ^js @main-window) (.maximize ^js @main-window))
    "openRemoteUrl" (.openExternal shell (.-data args))
    "writeToClipboard" (.write clipboard (.-data args)) ; SEE https://www.electronjs.org/docs/api/clipboard#clipboardwritedata-type
    "beginFrameSubscription" (.beginFrameSubscription (.-webContents ^js @main-window) false send-frames-to-renderer) ; SEE https://www.electronjs.org/docs/latest/api/web-contents#contentsbeginframesubscriptiononlydirty-callback
    "endFrameSubscription" (.endFrameSubscription (.-webContents ^js @main-window)) ; SEE https://www.electronjs.org/docs/latest/api/web-contents#contentsendframesubscription 
    "openDocument" (file/open)
    "saveDocument" (file/save (.-data args))))

(defn send-to-renderer
  ([action data]
   (.send (.-webContents ^js @main-window) "fromMain" #js {:action "windowEnteredFullscreen" :data data}))
  ([action]
   (send-to-renderer action nil)))

(defn init-main-window []
  (reset! main-window (BrowserWindow. (clj->js main-window-options)))

  (.once ^js @main-window "ready-to-show"
         #((.maximize ^js @main-window)
           (.show ^js @main-window)
           (.hide ^js @loading-window)
           (.close ^js @loading-window)))

  ;; Path is relative to the compiled js file (main.js in our case)
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))

  (doseq
   [[web-contents--event function]
    [["will-navigate"  #(.preventDefault %)] ;; Prevent navigation
     ["new-window" #(.preventDefault %)] ;; Prevent popups
     ["closed" #(reset! main-window nil)]]]
    (.on (.-webContents ^js @main-window) web-contents--event function))

  (when config/debug? (.openDevTools (.-webContents ^js @main-window)))

  (.on ipcMain "toMain" #(to-main-api %2))

  (doseq
   [[window-event function]
    [;; Event "resized" is probably more suitable, but it is not supported on linux
     ["resize" (send-to-renderer (if (.isMaximized ^js @main-window) "windowMaximized" "windowUnmaximized"))]
     ["maximize" (send-to-renderer "windowMaximized")]
     ["unmaximize" (send-to-renderer "windowUnmaximized")]
     ["enter-full-screen" (send-to-renderer "windowEnteredFullscreen")]
     ["leave-full-screen" (send-to-renderer "windowLeavedFullscreen")]
     ["minimize" (send-to-renderer "windowMinimized")]
     ["restore" (send-to-renderer "windowRestored")]]]
    (.on ^js @main-window window-event function))

  (.checkForUpdatesAndNotify updater))

(defn init-loading-window []
  (reset! loading-window (BrowserWindow. (clj->js loading-window-options)))
  (.once ^js @loading-window "show" init-main-window)
  (.loadURL ^js @loading-window (str "file://" js/__dirname "/public/loading.html"))
  (.once ^js @loading-window "ready-to-show" (.show ^js @loading-window)))

(defn main []
  (.init Sentry (clj->js config/sentry-options))
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on app "ready" init-loading-window))
