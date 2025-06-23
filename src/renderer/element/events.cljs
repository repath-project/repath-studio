(ns renderer.element.events
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.events :as-alias document.events]
   [renderer.effects :as-alias effects]
   [renderer.element.effects :as-alias element.effects]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.notification.events :as-alias notification.events]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.system :as utils.system]
   [renderer.window.effects :as-alias window.effects]))

(rf/reg-event-db
 ::select
 (fn [db [_ id multiple]]
   (-> (element.handlers/toggle-selection db id multiple)
       (history.handlers/finalize (if multiple
                                    #(t [::modify-selection "Modify selection"])
                                    #(t [::select-element   "Select elementd"]))))))

(rf/reg-event-db
 ::select-ids
 (fn [db [_ ids]]
   (-> (partial-right element.handlers/assoc-prop :selected true)
       (reduce (element.handlers/deselect-all db) ids)
       (history.handlers/finalize #(t [::select-elements "Select elements"])))))

(rf/reg-event-db
 ::toggle-prop
 (fn [db [_ id k]]
   (-> (element.handlers/update-prop db id k not)
       (history.handlers/finalize #(t [::toggle "Toggle %1"] [(name k)])))))

(rf/reg-event-db
 ::set-prop
 (fn [db [_ id k v]]
   (-> (element.handlers/assoc-prop db id k v)
       (history.handlers/finalize (str "Set " (name k))))))

(rf/reg-event-db
 ::lock
 (fn [db]
   (-> (element.handlers/assoc-prop db :locked true)
       (history.handlers/finalize "Lock selection"))))

(rf/reg-event-db
 ::unlock
 (fn [db]
   (-> (element.handlers/assoc-prop db :locked false)
       (history.handlers/finalize "Unlock selection"))))

(rf/reg-event-db
 ::set-attr
 (fn [db [_ k v]]
   (-> (element.handlers/set-attr db k v)
       (history.handlers/finalize (str "Set " (name k))))))

(rf/reg-event-db
 ::remove-attr
 (fn [db [_ k]]
   (-> (element.handlers/dissoc-attr db k)
       (history.handlers/finalize (str "Remove " (name k))))))

(rf/reg-event-db
 ::update-attr
 (fn [db [_ k f & more]]
   (-> (apply partial-right element.handlers/update-attr k f more)
       (reduce db (element.handlers/selected-ids db))
       (history.handlers/finalize (str "Update " (name k))))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (element.handlers/set-attr db k v)))

(rf/reg-event-db
 ::delete
 (fn [db]
   (-> (element.handlers/delete db)
       (history.handlers/finalize #(t [::delete-selection "Delete selection"])))))

(rf/reg-event-db
 ::deselect-all
 (fn [db]
   (-> (element.handlers/deselect-all db)
       (history.handlers/finalize "Deselect all"))))

(rf/reg-event-db
 ::select-all
 (fn [db]
   (-> (element.handlers/select-all db)
       (history.handlers/finalize "Select all"))))

(rf/reg-event-db
 ::select-same-tags
 (fn [db]
   (-> (element.handlers/select-same-tags db)
       (history.handlers/finalize "Select same tags"))))

(rf/reg-event-db
 ::invert-selection
 (fn [db]
   (-> (element.handlers/invert-selection db)
       (history.handlers/finalize "Invert selection"))))

(rf/reg-event-db
 ::raise
 (fn [db]
   (-> (element.handlers/update-index db inc)
       (history.handlers/finalize "Raise selection"))))

(rf/reg-event-db
 ::lower
 (fn [db]
   (-> (element.handlers/update-index db dec)
       (history.handlers/finalize "Lower selection"))))

(rf/reg-event-db
 ::raise-to-top
 (fn [db]
   (-> (element.handlers/update-index db (fn [_i sibling-count] (dec sibling-count)))
       (history.handlers/finalize "Raise selection to top"))))

(rf/reg-event-db
 ::lower-to-bottom
 (fn [db]
   (-> (element.handlers/update-index db #(identity 0))
       (history.handlers/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 ::align
 (fn [db [_ direction]]
   (-> (element.handlers/align db direction)
       (history.handlers/finalize (str "Update " direction)))))

(rf/reg-event-fx
 ::export-svg
 (fn [{:keys [db]} _]
   (let [els (element.handlers/root-children db)
         svg (utils.element/->svg els)]
     (if utils.system/electron?
       {::window.effects/ipc-invoke
        {:channel "export"
         :data svg
         :on-error [::notification.events/exception]}}
       {::effects/file-save
        [:data svg
         :on-error [::notification.events/exception]
         :options {:startIn "pictures"
                   :types [{:accept {"image/svg+xml" [".svg"]}}]}]}))))

(rf/reg-event-db
 ::paste
 (fn [db]
   (-> (element.handlers/paste db)
       (history.handlers/finalize "Paste selection"))))

(rf/reg-event-db
 ::paste-in-place
 (fn [db]
   (-> (element.handlers/paste-in-place db)
       (history.handlers/finalize "Paste selection in place"))))

(rf/reg-event-db
 ::paste-styles
 (fn [db]
   (-> (element.handlers/paste-styles db)
       (history.handlers/finalize "Paste styles to selection"))))

(rf/reg-event-db
 ::duplicate
 (fn [db]
   (-> (element.handlers/duplicate db)
       (history.handlers/finalize "Duplicate selection"))))

(rf/reg-event-db
 ::translate
 (fn [db [_ offset]]
   (-> (element.handlers/translate db offset)
       (history.handlers/finalize "Move selection"))))

(rf/reg-event-db
 ::place
 (fn [db [_ position]]
   (-> (element.handlers/place db position)
       (history.handlers/finalize "Place selection"))))

(rf/reg-event-db
 ::scale
 (fn [db [_ ratio]]
   (let [pivot-point (-> db element.handlers/bbox utils.bounds/center)]
     (-> (element.handlers/scale db ratio pivot-point false)
         (history.handlers/finalize "Scale selection")))))

(rf/reg-event-db
 ::move-up
 (fn [db _]
   (-> (element.handlers/translate db [0 -1])
       (history.handlers/finalize "Move selection up"))))

(rf/reg-event-db
 ::move-down
 (fn [db _]
   (-> (element.handlers/translate db [0 1])
       (history.handlers/finalize "Move selection down"))))

(rf/reg-event-db
 ::move-left
 (fn [db _]
   (-> (element.handlers/translate db [-1 0])
       (history.handlers/finalize "Move selection left"))))

(rf/reg-event-db
 ::move-right
 (fn [db [_]]
   (-> (element.handlers/translate db [1 0])
       (history.handlers/finalize "Move selection right"))))

(rf/reg-event-db
 ::->path
 (fn [db]
   (-> (element.handlers/->path db)
       (history.handlers/finalize "Convert selection to path"))))

(rf/reg-event-db
 ::stroke->path
 (fn [db]
   (-> (element.handlers/stroke->path db)
       (history.handlers/finalize "Convert selection's stroke to path"))))

(rf/reg-event-db
 ::boolean-operation
 (fn [db [_ operation]]
   (cond-> db
     (seq (rest (element.handlers/selected db)))
     (-> (element.handlers/boolean-operation operation)
         (history.handlers/finalize (string/capitalize (name operation)))))))

(rf/reg-event-db
 ::add
 (fn [db [_ el]]
   (-> (element.handlers/add db el)
       (history.handlers/finalize (str "Create " (name (:tag el)))))))

(rf/reg-event-db
 ::import-svg
 (fn [db [_ data]]
   (-> (element.handlers/import-svg db data)
       (history.handlers/finalize "Import svg"))))

(rf/reg-event-db
 ::animate
 (fn [db [_ tag attrs]]
   (-> (element.handlers/animate db tag attrs)
       (history.handlers/finalize (string/capitalize (name tag))))))

(rf/reg-event-db
 ::set-parent
 (fn [db [_ id parent-id]]
   (-> (element.handlers/set-parent db id parent-id)
       (history.handlers/finalize "Set parent"))))

(rf/reg-event-db
 ::group
 (fn [db]
   (-> (element.handlers/group db)
       (history.handlers/finalize "Group selection"))))

(rf/reg-event-db
 ::ungroup
 (fn [db]
   (-> (element.handlers/ungroup db)
       (history.handlers/finalize "Ungroup selection"))))

(rf/reg-event-db
 ::manipulate-path
 (fn [db [_ action]]
   (-> (element.handlers/manipulate-path db action)
       (history.handlers/finalize (-> (name action)
                                      (string/capitalize)
                                      (str " path"))))))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     {:db (element.handlers/copy db)
      :fx [(when (seq els)
             [::effects/clipboard-write
              {:data (utils.element/->svg els)
               :on-error [::notification.events/exception]}])]})))

(rf/reg-event-fx
 ::cut
 (fn [{:keys [db]} _]
   (let [els (element.handlers/top-selected-sorted db)]
     {:db (-> (element.handlers/copy db)
              (element.handlers/delete)
              (history.handlers/finalize "Cut selection"))
      :fx [(when (seq els)
             [::effects/clipboard-write
              {:data (utils.element/->svg els)
               :on-error [::notification.events/exception]}])]})))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (element.handlers/filter-by-tag db :image)]
     {::element.effects/trace images})))

(rf/reg-event-db
 ::traced
 (fn [db [_ data]]
   (-> (element.handlers/import-svg db data)
       (history.handlers/finalize "Trace image"))))

(rf/reg-event-fx
 ::import-file
 (fn [_ [_ file position]]
   (when-let [file-type (.-type file)]
     (cond
       (= file-type "image/svg+xml")
       {::app.effects/file-read-as
        [file :text {"load" {:formatter #(hash-map :svg %
                                                   :label (.-name file)
                                                   :position position)
                             :on-fire [::import-svg]}
                     "error" {:on-fire [::notification.events/exception]}}]}

       (contains? #{"image/jpeg" "image/png" "image/bmp" "image/gif"} file-type)
       {::element.effects/import-image [file position]}

       :else
       (let [extension (last (string/split (.-name file) "."))]
         (when (= extension "rps")
           {:dispatch [::document.events/file-read file]}))))))
