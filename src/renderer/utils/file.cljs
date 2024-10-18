(ns renderer.utils.file
  (:require
   [clojure.edn :as edn]
   [re-frame.core :as rf]
   [renderer.document.events :as-alias document.e]
   [renderer.notification.events :as-alias notification.e]))

(defn download!
  [{:keys [data title]}]
  (let [blob (js/Blob. [data])
        url (js/URL.createObjectURL blob)
        a (js/document.createElement "a")]
    (.setAttribute a "href" url)
    (.setAttribute a "download" title)
    (.click a)
    (js/window.URL.revokeObjectURL url)))

(defn legacy-open!
  [cb]
  (let [el (js/document.createElement "input")]
    (set! (.-type el) "file")
    (.addEventListener el "change" (fn [e] (.remove el)
                                     (cb (first (.. e -target -files)))))
    (.click el)))

(defn read!
  [^js/File file]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(let [document (-> (.. % -target -result)
                         (edn/read-string)
                         (assoc :title (.-name file)
                                :path (.-path file)))]
        (rf/dispatch [::document.e/load document])))
    (.readAsText reader file)))

(defn open!
  "https://developer.mozilla.org/en-US/docs/Web/API/Window/showOpenFilePicker"
  [{:keys [options callback]}]
  (let [callback (or callback read!)]
    (if (.-showOpenFilePicker js/window)
      (-> (.showOpenFilePicker js/window (clj->js options))
          (.then (fn [[^js/FileSystemFileHandle file-handle]]
                   (.then (.getFile file-handle) callback)))
          (.catch #(rf/dispatch [::notification.e/exception %])))
      (legacy-open! callback))))

(defn save!
  "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker"
  [{:keys [options data on-resolution formatter]}]
  (if (.-showSaveFilePicker js/window)
    (-> (.showSaveFilePicker js/window (clj->js options))
        (.then (fn [^js/FileSystemFileHandle file-handle]
                 (.then (.createWritable file-handle)
                        (fn [^js/FileSystemWritableFileStream writable-stream]
                          (.then (.write writable-stream data)
                                 (fn []
                                   (.close writable-stream)
                                   (when on-resolution
                                     (rf/dispatch [on-resolution (cond-> file-handle
                                                                   formatter
                                                                   formatter)]))))))))
        (.catch #(rf/dispatch [::notification.e/exception %])))
    (rf/dispatch
     [::notification.e/unavailable-feature
      "Save File Picker"
      "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"])))
