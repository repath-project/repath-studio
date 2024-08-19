(ns electron.printer
  (:require
   ["electron" :refer [BrowserWindow]]
   [promesa.core :as p]))

(defn send!
  [content]
  (let [window (BrowserWindow.
                #js {:show false
                     :frame false})]
    (.on (.-webContents window) "did-finish-load"
         #(.print
           (.-webContents window)
           #js {}
           (fn [success, error]
             (if success
               (p/resolved nil)
               (p/rejected error)))))

    (.loadURL window (str "data:text/html;charset=utf-8," content))))

