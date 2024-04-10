(ns file
  (:require
   ["electron" :refer [app dialog]]
   ["fs" :as fs]
   ["path" :as path]
   [config]
   [clojure.edn :as edn]
   [promesa.core :as p]))

(def dialog-options
  {:defaultPath (.getPath app "documents")
   :properties ["multiSelections"]
   :filters [{:name "rps"
              :extensions [config/ext]}]})

(defn- serialize-document
  [data file-path]
  (pr-str (assoc data
                 :path file-path
                 :title (.basename path file-path))))

(defn- write-file
  [file-path data]
  (.writeFileSync fs file-path (pr-str (dissoc data :closing? :path)) "utf-8")
  (p/resolved (serialize-document data file-path)))

(defn- read-file
  [file-path]
  (let [data (.readFileSync fs file-path "utf-8")
        document (edn/read-string data)]
    (serialize-document document file-path)))

(defn save-dialog
  [window options]
  (p/let [file (.showSaveDialog dialog window (clj->js options))
          file (get (js->clj file) "filePath")]
    (p/resolved file)))

(defn save-as
  [window data]
  (let [document (edn/read-string data)
        file-path (:path document)
        directory (and file-path (.dirname path file-path))
        dialog-options (cond-> dialog-options
                         (and directory (.existsSync fs directory))
                         (assoc :defaultPath directory))]
    (p/let [file (save-dialog window dialog-options)]
      (write-file file document))))

(defn save
  [window data]
  (let [document (edn/read-string data)
        file-path (:path document)]
    (if (and file-path (.existsSync fs file-path))
      (write-file file-path document)
      (save-as window data))))

(defn open
  [window file-path]
  (if (and file-path (.existsSync fs file-path))
    [(read-file file-path)]
    (p/let [files (.showOpenDialog dialog window (clj->js dialog-options))
            file-paths (get (js->clj files) "filePaths")]
      (p/resolved (map read-file file-paths)))))

(def export-options
  {:defaultPath (.getPath app "pictures")
   :filters [{:name "svg"
              :extensions ["svg" "svgo"]}]})

(defn export
  [window data]
  (p/let [file (save-dialog window export-options)]
    (.writeFileSync fs file data "utf-8")))
