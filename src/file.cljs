(ns file
  (:require
   ["electron" :refer [app dialog]]
   ["fs" :as fs]
   ["path" :as path]
   [clojure.edn :as edn]))

(def main-window (atom nil))

(def default-path (.getPath app "documents"))

(def dialog-options
  {:defaultPath default-path
   ;; https://www.electronjs.org/docs/api/structures/file-filter#filefilter-object
   :filters [{:name "rso"
              :extensions ["rso"]}]})

(defn serialize-document
  [data file-path]
  (pr-str (assoc data
                 :path file-path
                 :title (.basename path file-path))))

(defn write-file
  [file-path data f]
  (.writeFile fs file-path (pr-str data) #js {:encoding "utf-8"}
              (fn [_err] (f (serialize-document data file-path)))))

(defn save
  "Saves the provided data.
   
   If there is no path defined, pick a new file.
   https://www.electronjs.org/docs/api/dialog#dialogshowsavedialogbrowserwindow-options"
  [data f]
  (let [document (edn/read-string data)
        file-path (:path document)]
    (if (and file-path (.existsSync fs file-path))
      (write-file file-path document f)
      (.then (.showSaveDialog dialog ^js @main-window (clj->js dialog-options))
             (fn [^js/Promise file]
               (when-not (.-canceled file)
                 (write-file (.-filePath file) document f)))))))

(defn read-file
  [file-path f]
  (.readFile fs file-path #js {:encoding "utf-8"}
             (fn [_err data]
               (let [document (edn/read-string data)]
                 (f (serialize-document document file-path))))))

(defn open
  "Opens a file.
   https://www.electronjs.org/docs/api/dialog#dialogshowopendialogbrowserwindow-options"
  [file-path f]
  (if (and file-path (.existsSync fs file-path))
    (read-file file-path f)
    (.then (.showOpenDialog dialog ^js @main-window (clj->js dialog-options))
           (fn [^js/Promise file]
             (when-not (.-canceled file)
               (let [file-path (first (js->clj (.-filePaths file)))]
                 (read-file file-path f)))))))

(def export-options
  {:defaultPath default-path
   :filters [{:name "svg"
              :extensions ["svg" "svgo"]}]})

(defn export
  "Exports the provided data."
  [data]
  (.then (.showSaveDialog dialog ^js @main-window (clj->js export-options))
         (fn [^js/Promise file]
           (when-not (.-canceled file)
             (.writeFileSync fs (.-filePath file) data "utf-8")))))
