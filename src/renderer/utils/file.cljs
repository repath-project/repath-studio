(ns renderer.utils.file
  (:require
   [clojure.edn :as edn]
   [re-frame.core :as rf]))

(defn legacy-open!
  [cb]
  (let [el (js/document.createElement "input")]
    (set! (.-type el) "file")
    (.addEventListener el "change" #(do (.remove el)
                                        (cb (first (.. % -target -files)))))
    (.click el)))

(defn read!
  [^js/File file]
  (let [reader (js/FileReader.)]
    (.addEventListener
     reader
     "load"
     #(let [document (-> (.. % -target -result)
                         edn/read-string
                         (assoc :title (.-name file)
                                :path (.-path file)))]
        (rf/dispatch [:document/load document])))
    (.readAsText reader file)))

(defn open!
  "https://developer.mozilla.org/en-US/docs/Web/API/Window/showOpenFilePicker"
  ([file-picker-options]
   (open! file-picker-options read!))
  ([file-picker-options cb]
   (if (.-showOpenFilePicker js/window)
     (.then (.showOpenFilePicker js/window (clj->js file-picker-options))
            (fn [[^js/FileSystemFileHandle file-handle]]
              (.then (.getFile file-handle) cb)))
     (legacy-open! cb))))

(defn save!
  "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker"
  [file-picker-options cb]
  (if (.-showSaveFilePicker js/window)
    (.then (.showSaveFilePicker js/window (clj->js file-picker-options)) cb)
    (rf/dispatch
     [:notification/unavailable-feature
      "Save File Picker"
      "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"])))
