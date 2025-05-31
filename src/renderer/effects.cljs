(ns renderer.effects
  (:require
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.events]
   [renderer.utils.dom :as utils.dom]
   [renderer.utils.error :as utils.error]))

(rf/reg-cofx
 ::guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-fx
 ::clipboard-write
 (fn [{:keys [data on-success on-error]}]
   (-> (js/navigator.clipboard.write
        (array (js/ClipboardItem.
                (let [blob-array (js-obj)]
                  (doseq
                   [[data-type data] [["image/svg+xml" data]
                                      ["text/html" data]]]
                    (when (.supports js/ClipboardItem data-type)
                      (aset blob-array
                            data-type
                            (js/Blob. (array data) #js {:type data-type}))))
                  blob-array))))
       (.then #(when on-success (rf/dispatch on-success)))
       (.catch #(when on-error (rf/dispatch (conj on-error %)))))))

(rf/reg-fx
 ::focus
 (fn [id]
   (when-let [element (if id
                        (.getElementById js/document id)
                        (utils.dom/canvas-element!))]
     (.focus element))))

(rf/reg-fx
 ::set-document-attr
 (fn [[k v]]
   (.setAttribute js/window.document.documentElement k v)))

(defn legacy-file-open!
  [cb]
  (let [el (js/document.createElement "input")]
    (set! (.-type el) "file")
    (.addEventListener el "change" (fn [e]
                                     (.remove el)
                                     (cb (first (.. e -target -files)))))
    (.click el)))

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

(rf/reg-fx
 ::file-open
 (fn [{:keys [options on-error on-success]}]
   (let [success-cb #(rf/dispatch (conj on-success %))]
     (if (.-showOpenFilePicker js/window)
       (-> (.showOpenFilePicker js/window (clj->js options))
           (.then (fn [[^js/FileSystemFileHandle file-handle]]
                    (.then (.getFile file-handle) success-cb)))
           (.catch (fn [^js/Error error]
                     (when (and on-error (not (utils.error/abort-error? error)))
                       (rf/dispatch (conj on-error error))))))
       (legacy-file-open! success-cb)))))

(rf/reg-fx
 ::file-read-as
 (fn [[^js/File file method events]]
   (let [reader (js/FileReader.)]
     (doseq
      [[event {:keys [formatter on-fire]}] events]
       (.addEventListener reader event
                          #(rf/dispatch (conj on-fire
                                              (cond-> (.-result reader)
                                                formatter
                                                formatter)))))
     (case method
       :data-url (.readAsDataURL reader file)
       :text (.readAsText reader file)))))

(rf/reg-fx
 ::download
 (fn [{:keys [data title]}]
   (let [blob (js/Blob. [data])
         url (js/URL.createObjectURL blob)
         a (js/document.createElement "a")]
     (.setAttribute a "href" url)
     (.setAttribute a "download" title)
     (.click a)
     (js/window.URL.revokeObjectURL url))))

(rf/reg-fx
 ::scroll-into-view
 (fn [el]
   (.scrollIntoView el #js {:block "nearest"})))

(rf/reg-fx
 ::scroll-to-bottom
 (fn [el]
   (set! (.-scrollTop el) (.-scrollHeight el))))

(rf/reg-fx
 ::eye-dropper
 (fn [{:keys [on-success on-error]}]
   (-> (js/EyeDropper.)
       (.open)
       (.then #(when on-success (rf/dispatch (conj on-success %))))
       (.catch #(when on-error (rf/dispatch (conj on-error %)))))))

(rf/reg-fx
 ::print
 (fn [content]
   (let [print-window (.open js/window)
         document (.-document print-window)]
     (.write document content)
     (.print print-window)
     (.close print-window))))

(rf/reg-fx
 ::open-remote-url
 (fn [url]
   (.open js/window url)))

(rf/reg-fx
 ::add-listener
 (fn [[target channel listener formatter]]
   (.addEventListener target
                      channel
                      #(rf/dispatch (conj listener
                                          (cond-> % formatter formatter))))))

(rf/reg-fx
 ::ipc-send
 (fn [[channel data]]
   (js/window.api?.send channel (clj->js data))))

(rf/reg-fx
 ::ipc-invoke
 (fn [{:keys [channel data formatter on-success on-error]}]
   (-> (js/window.api?.invoke channel (clj->js data))
       (.then #(when on-success (rf/dispatch (conj on-success (cond-> % formatter formatter)))))
       (.catch #(when on-error (rf/dispatch (conj on-error %)))))))

(rf/reg-fx
 ::ipc-on
 (fn [[channel listener]]
   (js/window.api?.on channel #(rf/dispatch listener))))
