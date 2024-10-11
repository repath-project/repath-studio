(ns renderer.app.effects
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.db :as db]
   [renderer.app.events :as-alias e]
   [renderer.history.handlers :as history.h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.utils.dom :as dom]
   [renderer.utils.drop :as drop]
   [renderer.utils.file :as file]))

(rf.storage/reg-co-fx! config/app-key {:cofx :store})

(defn persist!
  [data]
  (rf.storage/->store config/app-key (-> data
                                         (history.h/drop-rest)
                                         (select-keys db/persistent-keys))))

(def persist
  "This is a modified version of akiroz.re-frame.storage/persist-db-keys
   The before key is removed and we are dropping the rest of the history states
   to get performance to an acceptable level and minimize resource allocation."
  (rf/->interceptor
   :id ::persist
   :after (fn [context]
            (when-let [data (get-in context [:effects :db])]
              (persist! data))
            context)))

(def custom-fx
  (rf/->interceptor
   :id ::custom-fx
   :after (fn [context]
            (let [db (rf/get-effect context :db ::not-found)]
              (cond-> context
                (not= db ::not-found)
                (-> (rf/assoc-effect :fx (apply conj (or (:fx (rf/get-effect context)) []) (:fx db)))
                    (rf/assoc-effect :db (assoc db :fx []))))))))

(rf/reg-global-interceptor custom-fx)

(rf/reg-cofx
 ::guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-cofx
 ::now
 (fn [coeffects _]
   (assoc coeffects :now (.now js/Date))))

(rf/reg-fx
 ::data-transfer
 (fn [[position data-transfer]]
   (drop/items! position (.-items data-transfer))
   (drop/files! position (.-files data-transfer))))

(rf/reg-fx
 ::set-pointer-capture
 (fn [pointer-id]
   (.setPointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::release-pointer-capture
 (fn [pointer-id]
   (.releasePointerCapture (dom/canvas-element!) pointer-id)))

(rf/reg-fx
 ::persist
 (fn [data]
   (persist! data)))

(rf/reg-fx
 ::local-storage-clear
 (fn []
   (rf.storage/->store config/app-key {})))

(rf/reg-fx
 ::clipboard-write
 (fn [data]
   (when data
     (js/navigator.clipboard.write
      (array (js/ClipboardItem.
              (let [blob-array (js-obj)]
                (doseq
                 [[data-type data]
                  [["image/svg+xml" data]
                   ["text/html" data]
                   ["text/plain" data]]]
                  (when (.supports js/ClipboardItem data-type)
                    (aset blob-array data-type (js/Blob. (array data) #js {:type data-type}))))
                blob-array)))))))

(rf/reg-fx
 ::focus
 (fn [id]
   (when-let [element (if id (.getElementById js/document id) (dom/canvas-element!))]
     (js/setTimeout #(.focus element)))))

(rf/reg-fx
 ::query-local-fonts
 (fn [{:keys [on-resolution formatter]}]
   (when-not (undefined? js/window.queryLocalFonts)
     (-> (.queryLocalFonts js/window)
         (.then #(rf/dispatch [on-resolution (cond-> % formatter formatter)]))))))

(rf/reg-fx
 ::save
 file/save!)

(rf/reg-fx
 ::open
 file/open!)

(rf/reg-fx
 ::download
 file/download!)

(rf/reg-fx
 ::set-document-attr
 (fn [[k v]]
   (.setAttribute js/window.document.documentElement k v)))
