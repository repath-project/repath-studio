(ns file
  (:require
   ["electron" :refer [app dialog]]
   ["fs" :as fs]
   ["path" :as path]
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
   :filters [{:name "edn"
              :extensions ["edn"]}]})

(defn save
  "Saves the provided data.
   https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogsyncbrowserwindow-options"
  [data]
  (.then (.showSaveDialog dialog ^js @main-window (clj->js dialog-options))
         (fn [^js/Promise file]
           (when-not (.-canceled file)
             (.writeFileSync fs (.-filePath file) data "utf-8")))))

(defn open
  "Opens a file.
   https://www.electronjs.org/docs/api/dialog#dialogshowopendialogsyncbrowserwindow-options"
  [f]
  (.then (.showOpenDialog dialog ^js @main-window (clj->js dialog-options))
         (fn [^js/Promise file]
           (when-not (.-canceled file)
             (let [file-path (first (js->clj (.-filePaths file)))]
               (.readFile fs file-path #js {:encoding "utf-8"}
                          (fn [_err data] (f {:path file-path
                                              :name (.basename path file-path)
                                              :data data}))))))))

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
