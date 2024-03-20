(ns renderer.utils.file
  (:require
   [re-frame.core :as rf]))

(defn open!
  [file-picker-options cb]
  (if (.-showOpenFilePicker js/window)
    (.then (.showOpenFilePicker js/window (clj->js file-picker-options)) cb)
    (rf/dispatch
     [:notification/unavailable-feature
      "Open File Picker"
      "https://developer.mozilla.org/en-US/docs/Web/API/window/showOpenFilePicker#browser_compatibility"])))

(defn save!
  [file-picker-options cb]
  (if (.-showSaveFilePicker js/window)
    (.then (.showSaveFilePicker js/window (clj->js file-picker-options)) cb)
    (rf/dispatch
     [:notification/unavailable-feature
      "Save File Picker"
      "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"])))
