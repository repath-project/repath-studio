(ns electron.file
  (:require
   ["electron" :refer [app dialog BrowserWindow]]
   ["fs" :as fs]
   ["path" :as path]
   [clojure.edn :as edn]
   [config :as config]
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

(defn- write-file!
  [file-path data]
  (.writeFileSync fs file-path (pr-str (dissoc data :path :id)) "utf-8")
  (p/resolved (serialize-document (select-keys data [:id]) file-path)))

(defn- read!
  [file-path]
  (let [data (.readFileSync fs file-path "utf-8")
        document (edn/read-string data)]
    (serialize-document document file-path)))

(defn save-dialog!
  [options]
  (p/let [file (.showSaveDialog dialog (clj->js options))
          file (get (js->clj file) "filePath")]
    (p/resolved file)))

(defn save-as!
  [data]
  (let [document (edn/read-string data)
        file-path (:path document)
        directory (and file-path (.dirname path file-path))
        options (cond-> dialog-options
                  (and directory (.existsSync fs directory))
                  (assoc :defaultPath directory)

                  :always
                  (update :defaultPath #(.join path % (:title document))) )]
    (p/let [file (save-dialog! options)]
      (write-file! file document))))

(defn save!
  [data]
  (let [document (edn/read-string data)
        file-path (:path document)]
    (if (and file-path (.existsSync fs file-path))
      (write-file! file-path document)
      (save-as! data))))

(defn open!
  [file-path]
  (if (and file-path (.existsSync fs file-path))
    (array (read! file-path))
    (p/let [files (.showOpenDialog dialog (clj->js dialog-options))
            file-paths (get (js->clj files) "filePaths")]
      (p/resolved (clj->js (mapv read! file-paths))))))

(def export-options
  {:defaultPath (.getPath app "pictures")
   :filters [{:name "svg"
              :extensions ["svg"]}]})

(defn export!
  [data]
  (p/let [file (save-dialog! export-options)]
    (.writeFile fs file data "utf-8" (fn [err]
                                       (if err
                                         (p/rejected err)
                                         (p/resolved data))))))

(defn print!
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
