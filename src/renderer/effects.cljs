(ns renderer.effects
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.events]
   [renderer.utils.dom :as utils.dom]))

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

(defn write-file!
  [{:keys [data on-success on-error formatter file-handle]}]
  (-> (.createWritable file-handle)
      (.then (fn [^js/FileSystemWritableFileStream writable-stream]
               (-> (.write writable-stream data)
                   (.then (fn []
                            (.close writable-stream)
                            (when on-success
                              (rf/dispatch (conj on-success (cond-> file-handle
                                                              formatter
                                                              formatter)))))))))
      (.catch #(when on-error (rf/dispatch (conj on-error %))))))

(defn- abort-error?
  [error]
  (string/includes? (.-message error) "The user aborted a request."))

(rf/reg-fx
 ::file-save
 (fn [{:keys [options on-error file-handle]
       :as args}]
   (if file-handle
     (write-file! args)
     (if (.-showSaveFilePicker js/window)
       (-> (.showSaveFilePicker js/window (clj->js options))
           (.then #(write-file! (assoc args :file-handle %)))
           (.catch (fn [^js/Error error]
                     (when (and on-error (not (abort-error? error)))
                       (rf/dispatch (conj on-error error))))))
       (rf/dispatch
        [::notification.events/show-unavailable-feature
         "Save File Picker"
         "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"])))))

(rf/reg-fx
 ::file-open
 (fn [{:keys [options on-error on-success]}]
   (if (.-showOpenFilePicker js/window)
     (-> (.showOpenFilePicker js/window (clj->js options))
         (.then (fn [file-handles]
                  (when on-success
                    (doseq [^js/FileSystemFileHandle file-handle file-handles]
                      (-> (.getFile file-handle)
                          (.then #(rf/dispatch (conj on-success file-handle %)))
                          (.catch #(when on-error
                                     (rf/dispatch (conj on-error file-handle %)))))))))
         (.catch (fn [^js/Error error]
                   (when (and on-error (not (abort-error? error)))
                     (rf/dispatch (conj on-error error))))))
     (legacy-file-open! #(rf/dispatch (conj on-success nil %))))))

(rf/reg-fx
 ::file-read-as
 (fn [[^js/File file read-as events]]
   (let [reader (js/FileReader.)]
     (doseq
      [[event {:keys [formatter on-fire]}] events]
       (.addEventListener reader event
                          #(rf/dispatch (conj on-fire
                                              (cond-> (.-result reader)
                                                formatter
                                                formatter)))))
     (case read-as
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
 (fn [dom-el]
   (.scrollIntoView dom-el #js {:block "nearest"})))

(rf/reg-fx
 ::scroll-to-bottom
 (fn [dom-el]
   (set! (.-scrollTop dom-el) (.-scrollHeight dom-el))))

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
 ::add-event-listener
 (fn [[target channel event formatter]]
   (.addEventListener target channel #(rf/dispatch (conj event (cond-> %
                                                                 formatter
                                                                 formatter))))))

(rf/reg-fx
 ::ipc-send
 (fn [[channel data]]
   (when js/window.api
     (js/window.api.send channel (clj->js data)))))

(rf/reg-fx
 ::ipc-invoke
 (fn [{:keys [channel data formatter on-success on-error]}]
   (when js/window.api
     (-> (js/window.api.invoke channel (clj->js data))
         (.then #(when on-success (rf/dispatch (conj on-success (cond-> %
                                                                  formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))

(rf/reg-fx
 ::ipc-on
 (fn [[channel listener]]
   (when js/window.api
     (js/window.api.on channel #(rf/dispatch listener)))))
