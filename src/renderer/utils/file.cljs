(ns renderer.utils.file
  (:require
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.e]))

#_(def JSFile
    [:fn (fn [x]
           (if-not (nil? x)
             (identical? (.-constructor x) js/File)
             false))])

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

(defn open!
  "https://developer.mozilla.org/en-US/docs/Web/API/Window/showOpenFilePicker"
  [{:keys [options on-error on-success]}]
  (let [success-cb #(rf/dispatch (conj on-success %))]
    (if (.-showOpenFilePicker js/window)
      (-> (.showOpenFilePicker js/window (clj->js options))
          (.then (fn [[^js/FileSystemFileHandle file-handle]]
                   (.then (.getFile file-handle) success-cb)))
          (.catch #(when on-error (rf/dispatch (conj on-error %)))))
      (legacy-open! success-cb))))

(defn save!
  "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker"
  [{:keys [options data on-success on-error formatter]}]
  (if (.-showSaveFilePicker js/window)
    (-> (.showSaveFilePicker js/window (clj->js options))
        (.then (fn [^js/FileSystemFileHandle file-handle]
                 (.then (.createWritable file-handle)
                        (fn [^js/FileSystemWritableFileStream writable-stream]
                          (.then (.write writable-stream data)
                                 (fn []
                                   (.close writable-stream)
                                   (when on-success
                                     (rf/dispatch (conj on-success (cond-> file-handle
                                                                     formatter
                                                                     formatter))))))))))
        (.catch #(when on-error (rf/dispatch (conj on-error %)))))
    (rf/dispatch
     [::notification.e/unavailable-feature
      "Save File Picker"
      "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"])))
