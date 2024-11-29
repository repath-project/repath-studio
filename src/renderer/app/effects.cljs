(ns renderer.app.effects
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.db :as db]
   [renderer.app.events :as-alias e]
   [renderer.history.handlers :as history.h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.utils.dom :as dom]))

(rf.storage/reg-co-fx! config/app-key {:cofx :store})

(rf/reg-cofx
 ::guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-cofx
 ::now
 (fn [coeffects _]
   (assoc coeffects :now (.now js/Date))))

(rf/reg-fx
 ::persist
 (fn [db]
   (let [db (cond-> db
              (:active-document db)
              history.h/drop-rest)]
     (->> (select-keys db db/persistent-keys)
          (rf.storage/->store config/app-key)))))

(rf/reg-fx
 ::local-storage-clear
 (fn []
   (rf.storage/->store config/app-key {})))

(rf/reg-fx
 ::clipboard-write
 (fn [{:keys [data on-success on-error]}]
   (-> (js/navigator.clipboard.write
        (array (js/ClipboardItem.
                (let [blob-array (js-obj)]
                  (doseq
                   [[data-type data]
                    [["image/svg+xml" data]
                     ["text/html" data]]]
                    (when (.supports js/ClipboardItem data-type)
                      (aset blob-array data-type (js/Blob. (array data) #js {:type data-type}))))
                  blob-array))))
       (.then #(when on-success (rf/dispatch on-success)))
       (.catch #(when on-error (rf/dispatch (conj on-error %)))))))

(rf/reg-fx
 ::focus
 (fn [id]
   (when-let [element (if id (.getElementById js/document id) (dom/canvas-element!))]
     (.focus element))))

(rf/reg-fx
 ::query-local-fonts
 (fn [{:keys [on-success on-error formatter]}]
   (when-not (undefined? js/window.queryLocalFonts)
     (-> (.queryLocalFonts js/window)
         (.then #(when on-success (rf/dispatch (conj on-success (cond-> % formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))

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
                                      (rf/dispatch (conj on-success (cond-> file-handle
                                                                      formatter
                                                                      formatter))))))))))
         (.catch #(when on-error (rf/dispatch (conj on-error %)))))
     (rf/dispatch
      [::notification.e/unavailable-feature
       "Save File Picker"
       "https://developer.mozilla.org/en-US/docs/Web/API/Window/showSaveFilePicker#browser_compatibility"]))))

(defn legacy-file-open!
  [cb]
  (let [el (js/document.createElement "input")]
    (set! (.-type el) "file")
    (.addEventListener el "change" (fn [e] (.remove el)
                                     (cb (first (.. e -target -files)))))
    (.click el)))

(rf/reg-fx
 ::file-open
 (fn [{:keys [options on-error on-success]}]
   (let [success-cb #(rf/dispatch (conj on-success %))]
     (if (.-showOpenFilePicker js/window)
       (-> (.showOpenFilePicker js/window (clj->js options))
           (.then (fn [[^js/FileSystemFileHandle file-handle]]
                    (.then (.getFile file-handle) success-cb)))
           (.catch #(when on-error (rf/dispatch (conj on-error %)))))
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
 ::set-document-attr
 (fn [[k v]]
   (.setAttribute js/window.document.documentElement k v)))

(rf/reg-fx
 ::validate-db
 (fn [[db event]]
   (when (not (db/valid? db))
     (js/console.error (str "Event: " (first event)))
     (throw (js/Error. (str "Spec check failed: " (db/explain db)))))))

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
