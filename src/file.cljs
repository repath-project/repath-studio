(ns file
  (:require
   ["electron" :refer [app dialog]]
   ["fs" :as fs]
   ["path" :as path]
   [clojure.edn :as edn]
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
   :filters [{:name "rso"
              :extensions ["rso"]}]})

(defn write-to-file
  [file-path data f]
  (.writeFile fs file-path data #js {:encoding "utf-8"}
              (fn [_err] (f {:path file-path
                             :title (.basename path file-path)
                             :data data}))))

(defn save
  "Saves the provided data.
   https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogsyncbrowserwindow-options"
  [data f]
  (let [file-path (-> data edn/read-string :path)]
    (if (and file-path (.existsSync fs file-path))
      (write-to-file file-path data f)
      (.then (.showSaveDialog dialog ^js @main-window (clj->js dialog-options))
             (fn [^js/Promise file]
               (when-not (.-canceled file)
                 (write-to-file (.-filePath file) data f)))))))

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
                                              :title (.basename path file-path)
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
