(ns electron.file
  (:require
   ["electron" :refer [app dialog BrowserWindow]]
   ["fs" :as fs]
   ["path" :as path]
   [clojure.edn :as edn]
   [config :as config]))

(def dialog-options
  {:defaultPath (.getPath app "documents")
   :properties ["multiSelections"]
   :filters [{:name config/ext
              :extensions [config/ext]}]})

(defn- serialize-document
  [data file-path]
  (pr-str (assoc data
                 :path file-path
                 :title (.basename path file-path))))

(defn- write-file!
  [file-path data]
  (let [document (pr-str (dissoc data :path :id :title))]
    (.writeFileSync fs file-path document "utf-8")
    (js/Promise.resolve (serialize-document (select-keys data [:id]) file-path))))

(defn- read!
  [file-path]
  (let [data (.readFileSync fs file-path "utf-8")
        document (edn/read-string data)]
    (serialize-document document file-path)))

(defn save-dialog!
  [options]
  (-> (.showSaveDialog dialog (clj->js options))
      (.then (fn [^js/Object result]
               (when-not (.-canceled result)
                 (-> result js->clj (get "filePath")))))))

(defn save-as!
  [data]
  (let [document (edn/read-string data)
        file-path (:path document)
        directory (and file-path (.dirname path file-path))
        options (cond-> dialog-options
                  (and directory (.existsSync fs directory))
                  (assoc :defaultPath directory)

                  :always
                  (update :defaultPath #(.join path % (:title document))))]
    (-> (save-dialog! options)
        (.then (fn [file-path]
                 (when file-path
                   (write-file! file-path document)))))))

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
    (-> (.showOpenDialog dialog (clj->js dialog-options))
        (.then (fn [^js/Object result]
                 (when-not (.-canceled result)
                   (->> (get (js->clj result) "filePaths")
                        (mapv read!)
                        (clj->js)
                        (js/Promise.resolve))))))))

(def export-options
  {:defaultPath (.getPath app "pictures")
   :filters [{:name "svg"
              :extensions ["svg"]}]})

(defn export!
  [data]
  (-> (save-dialog! export-options)
      (.then (fn [file-path]
               (when file-path
                 (.writeFile fs file-path data "utf-8" (fn [error]
                                                         (if error
                                                           (js/Promise.reject error)
                                                           (js/Promise.resolve data)))))))))

(defn print!
  [content]
  (let [window (BrowserWindow. #js {:show false
                                    :frame false})]
    (js/Promise.
     (fn [res rej]
       (.on (.-webContents window) "did-finish-load"
            #(.print
              (.-webContents window)
              #js {}
              (fn [success error]
                (if success
                  (res)
                  (rej error)))))

       (.loadURL window (str "data:text/html;charset=utf-8," content))))))
