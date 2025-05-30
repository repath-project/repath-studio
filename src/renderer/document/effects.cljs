(ns renderer.document.effects
  (:require
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.events]
   [renderer.utils.error :as utils.error]))

(rf/reg-fx
 ::file-save
 (fn [{:keys [options data on-success on-error formatter]}]
   (if (.-showSaveFilePicker js/window)
     (-> (.showSaveFilePicker js/window (clj->js options))
         (.then (fn [^js/FileSystemFileHandle file-handle]
                  (.then (.createWritable file-handle)
                         (fn [^js/FileSystemWritableFileStream writable-stream]
                           (.then (.write writable-stream data)
                                  (fn []
                                    (.close writable-stream)
                                    (when on-success
                                      (rf/dispatch (conj on-success
                                                         (cond-> file-handle
                                                           formatter
                                                           formatter))))))))))
         (.catch (fn [^js/Error error]
                   (when (and on-error (not (utils.error/abort-error? error)))
                     (rf/dispatch (conj on-error error))))))
     (rf/dispatch
      [::notification.events/show-unavailable-feature
       "Save File Picker"
       "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"]))))
