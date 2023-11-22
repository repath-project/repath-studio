(ns file
  (:require
   ["electron" :refer [app dialog]]
   ["fs" :as fs]
   #_[cognitect.transit :as tr]))

(def main-window (atom nil))

#_(defn roundtrip
    [data]
    (let [writer (tr/writer :json)]
      (tr/write writer data)))

(def default-path (.getPath app "documents"))

(def dialog-options
  {:defaultPath default-path
   ;; https://www.electronjs.org/docs/api/structures/file-filter#filefilter-object
   :filters [{:name "rp"
              :extensions ["rp"]}]})

(defn save
  "Saves the provided data.
   https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogsyncbrowserwindow-options"
  [data]
  (.then (.showSaveDialog dialog ^js @main-window (clj->js dialog-options))
         (fn [^js/Promise file]
           (when-not (.-canceled file)
             (.writeFileSync fs (.-filePath file) data "utf-8")))))

(defn open-to-renderer
  [_err data]
  (let [web-contents (.-webContents ^js @main-window)]
    (.send web-contents "fromMain" (clj->js {:action "openDocument" :data data}))))

(defn open
  "Opens a file.
   https://www.electronjs.org/docs/api/dialog#dialogshowopendialogsyncbrowserwindow-options"
  []
  (.then (.showOpenDialog dialog ^js @main-window (clj->js dialog-options))
         (fn [^js/Promise file]
           (when-not (.-canceled file)
             (.readFileSync fs (.-filePath file) "utf-8" open-to-renderer)))))

(def export-options
  {:defaultPath default-path
   :filters [{:name "svg"
              :extensions ["svg" "svgo"]}
             {:name "png"
              :extensions ["png" "svgo"]}
             {:name "jpg"
              :extensions ["jpg" "jpeg"]}]})

(defn export
  "Exports the provided data."
  [data]
  (.then (.showSaveDialog dialog ^js @main-window (clj->js export-options))
         (fn [^js/Promise file]
           (when-not (.-canceled file)
             (.writeFileSync fs (.-filePath file) data "utf-8")))))
