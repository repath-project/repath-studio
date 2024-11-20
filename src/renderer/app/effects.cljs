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
   [renderer.utils.file :as file]))

(rf.storage/reg-co-fx! config/app-key {:cofx :store})

(def persist
  (rf/->interceptor
   :id ::persist
   :after (fn [context]
            (let [db (rf/get-effect context :db)
                  fx (rf/get-effect context :fx)]
              (cond-> context
                db
                (rf/assoc-effect :fx (conj (or fx []) [::persist db])))))))

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
   (let [db (cond-> db (:active-document db) history.h/drop-rest)]
     (rf.storage/->store config/app-key (select-keys db db/persistent-keys)))))

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
 (fn [{:keys [on-success on-error formatter]}]
   (when-not (undefined? js/window.queryLocalFonts)
     (-> (.queryLocalFonts js/window)
         (.then #(when on-success (rf/dispatch (conj on-success (cond-> % formatter formatter)))))
         (.catch #(when on-error (rf/dispatch (conj on-error %))))))))

(rf/reg-fx
 ::file-save
 file/save!)

(rf/reg-fx
 ::file-open
 file/open!)

(rf/reg-fx
 ::download
 file/download!)

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

(def schema-validator
  (rf/->interceptor
   :id ::schema-validator
   :after (fn [context]
            (let [db (or (rf/get-effect context :db)
                         (rf/get-coeffect context :db))
                  fx (rf/get-effect context :fx)
                  event (rf/get-coeffect context :event)]
              (cond-> context
                db
                (rf/assoc-effect :fx (conj (or fx []) [::validate-db [db event]])))))))
