(ns electron.main
  "https://www.electronjs.org/docs/latest/tutorial/process-model#the-main-process"
  (:require
   ["electron" :refer [app shell ipcMain BrowserWindow]]
   ["electron-log/main" :as log]
   ["electron-window-state" :as window-state-keeper]
   ["os" :as os]
   ["path" :as path]
   ["url" :as url]
   [config :as config]
   [electron.file :as file]))

(defonce main-window (atom nil))
(defonce loading-window (atom nil))

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
  [^js/Url url]
  (contains? allowed-urls (.-host url)))

(defn secure-url?
  [^js/Url url]
  (= (.-protocol url) "https:"))

(defn open-external!
  [url]
  (let [url-parsed (js/URL. url)]
    (when (and (secure-url? url-parsed) (allowed-url? url-parsed))
      (.openExternal shell url-parsed.href))))

(defn register-ipc-on-events! []
  (doseq
   [[e f]
    [["initialized" #(.close ^js @loading-window)]
     ["relaunch" #(doto app (.relaunch) (.exit))]
     ["open-remote-url" open-external!]
     ["open-directory" #(.showItemInFolder shell %)]
     ["window-minimize" #(.minimize ^js @main-window)]
     ["window-toggle-fullscreen" #(.setFullScreen ^js @main-window
                                                  (not (.isFullScreen ^js @main-window)))]
     ["window-toggle-maximized" #(if (.isMaximized ^js @main-window)
                                   (.unmaximize ^js @main-window)
                                   (.maximize ^js @main-window))]]]
    (.on ipcMain e #(f %2))))

(defn register-ipc-handle-events! []
  (doseq
   [[e f]
    [["open-documents" file/open!]
     ["save-document" file/save!]
     ["save-document-as" file/save-as!]
     ["export" file/export!]
     ["print" file/print!]]]
    (.handle ipcMain e #(f %2))))

(defn register-window-events! []
  (doseq
   [[window-event f]
    [["maximize" #(send-to-renderer! "window-maximized")]
     ["unmaximize" #(send-to-renderer! "window-unmaximized")]
     ["enter-full-screen" #(send-to-renderer! "window-entered-fullscreen")]
     ["leave-full-screen" #(send-to-renderer! "window-leaved-fullscreen")]
     ["minimize" #(send-to-renderer! "window-minimized")]
     ["restore" #(send-to-renderer! "window-restored")]
     ["resize" #(send-to-renderer! (if (.isMaximized ^js @main-window)
                                     "window-maximized"
                                     "window-unmaximized"))]]]
    (.on ^js @main-window window-event f)))

(defn register-web-contents-events! []
  (let [web-contents (.-webContents ^js @main-window)]
    (doseq
     [[web-contents-event f]
      [["will-frame-navigate" #(.preventDefault %)]
       ["closed" #(reset! main-window nil)]]]
      (.on web-contents web-contents-event f))))

(defn on-ready-to-show!
  [^js window]
  (doseq
   [action
    [(if (.isMaximized window) "window-maximized" "window-unmaximized")
     (if (.isFullScreen window) "window-entered-fullscreen" "window-leaved-fullscreen")]]
    (send-to-renderer! action)))

(defn resource-path
  [s]
  (url/format #js {:pathname (.join path js/__dirname s)
                   :protocol "file"}))

(defn init-main-window! []
  (let [win-state (window-state-keeper #js {:defaultWidth 1920
                                            :defaultHeight 1080})]
    (reset! main-window
            (BrowserWindow.
             #js {:x (.-x win-state)
                  :y (.-y win-state)
                  :width (.-width win-state)
                  :height (.-height win-state)
                  :titleBarStyle (when (= (.platform os) "darwin") "hidden")
                  :trafficLightPosition #js {:x 8 :y 10}
                  :icon (resource-path "/public/img/icon.png")
                  :frame false
                  :show false
                  :transparent true
                  :webPreferences
                  #js {:sandbox false
                       :preload (.join path js/__dirname "preload.js")}}))

    (.once ^js @main-window
           "ready-to-show"
           (fn []
             (.show ^js @main-window)
             (.manage win-state ^js @main-window)))

    (.on ^js @main-window "ready-to-show" #(on-ready-to-show! @main-window))

    (.loadURL ^js @main-window
              (if config/debug?
                "http://localhost:8080"
                (resource-path "/public/index.html")))

    (register-web-contents-events!)
    (register-ipc-on-events!)
    (register-ipc-handle-events!)
    (register-window-events!)))

(defn init-loading-window! []
  (set! (.-allowRendererProcessReuse app) false)
  (reset! loading-window
          (BrowserWindow.
           #js {:width 720
                :height 576
                :icon (resource-path "/public/img/icon.png")
                :show false
                :alwaysOnTop true
                :transparent true
                :frame false}))
  (.once ^js @loading-window "show" init-main-window!)
  (.loadURL ^js @loading-window (resource-path "/public/loading.html"))
  (.once ^js (.-webContents @loading-window)
         "did-finish-load"
         #(.show ^js @loading-window)))

(defn ^:export init! []
  (.initialize log)
  (.on app "window-all-closed" #(when-not (= js/process.platform "darwin") (.quit app)))
  (.on app "ready" init-loading-window!))
