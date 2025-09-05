(ns electron.preload
  "https://www.electronjs.org/docs/latest/tutorial/tutorial-preload"
  (:require
   ["electron" :refer [contextBridge ipcRenderer]]))

(def api
  #js {;; Strip event as it includes `sender`.
       ;; https://www.electronjs.org/docs/latest/api/ipc-renderer#ipcrendereronchannel-listener
       :on (fn [channel f] (.on ipcRenderer channel (fn [_event args] (f args))))
       :send (fn [channel args] (.send ipcRenderer channel args))
       :invoke (fn [channel args] (.invoke ipcRenderer channel args))
       :platform (.-platform js/process)
       :versions (.-versions js/process)
       :env (.-env js/process)})

(defn ^:export init!
  "Expose protected methods that allow the renderer process to use the ipcRenderer.
   https://www.electronjs.org/docs/api/context-bridge"
  []
  (.exposeInMainWorld contextBridge "api" api))
