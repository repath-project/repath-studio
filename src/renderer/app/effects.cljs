(ns renderer.app.effects
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [renderer.app.db :as db]
   [renderer.app.events :as-alias e]
   [renderer.utils.data-transfer :as data-transfer]
   [renderer.utils.dom :as dom]))

(rf.storage/reg-co-fx! config/app-key {:cofx :store})

(rf/reg-cofx
 ::now
 (fn [coeffects _]
   (assoc coeffects :now (.now js/Date))))

(rf/reg-cofx
 ::guid
 (fn [coeffects _]
   (assoc coeffects :guid (random-uuid))))

(rf/reg-fx
 ::data-transfer
 (fn [[position data-transfer]]
   (data-transfer/items! position (.-items data-transfer))
   (data-transfer/files! position (.-files data-transfer))))

(rf/reg-fx
 ::set-pointer-capture
 (fn [[target pointer-id]]
   (.setPointerCapture target pointer-id)))

(rf/reg-fx
 ::local-storage-persist
 (fn [data]
   (rf.storage/->store config/app-key (select-keys data db/persistent-keys))))

(rf/reg-fx
 ::local-storage-clear
 (fn []
   (rf.storage/->store config/app-key {})))

(rf/reg-fx
 ::clipboard-write
 (fn [[data]]
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
   (when-let [element (if id (.getElementById js/document id) (dom/canvas-element))]
     (js/setTimeout #(.focus element)))))

(rf/reg-fx
 ::load-system-fonts
 (fn []
   (when-not (undefined? js/window.queryLocalFonts)
     (p/let [fonts (.queryLocalFonts js/window)]
       (rf/dispatch [::e/set-system-fonts
                     (mapv (fn [^js/FontData font-data]
                             (into {} [[:postscriptName (.-postscriptName font-data)]
                                       [:fullName (.-fullName ^js font-data)]
                                       [:family (.-family ^js font-data)]
                                       [:style (.-style ^js font-data)]])) fonts)])))))

