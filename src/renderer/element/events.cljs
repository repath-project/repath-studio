(ns renderer.element.events
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.effects :as-alias fx]
   [renderer.element.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.utils.system :as system]
   [renderer.window.effects :as-alias window.fx]))

(rf/reg-event-db
 ::select
 (fn [db [_ id multiple]]
   (-> (h/select db id multiple)
       (history.h/finalize "Select element"))))

(rf/reg-event-db
 ::select-ids
 (fn [db [_ ids]]
   (-> (reduce (partial-right h/assoc-prop :selected true) (h/deselect db) ids)
       (history.h/finalize "Select elements"))))

(rf/reg-event-db
 ::toggle-prop
 (fn [db [_ id k]]
   (-> (h/update-prop db id k not)
       (history.h/finalize (str "Toggle " (name k))))))

(rf/reg-event-db
 ::preview-prop
 (fn [db [_ id k v]]
   (h/assoc-prop db id k v)))

(rf/reg-event-db
 ::set-prop
 (fn [db [_ id k v]]
   (-> (h/assoc-prop db id k v)
       (history.h/finalize (str "Set " (name k))))))

(rf/reg-event-db
 ::lock
 (fn [db]
   (-> (h/assoc-prop db :locked true)
       (history.h/finalize "Lock selection"))))

(rf/reg-event-db
 ::unlock
 (fn [db]
   (-> (h/assoc-prop db :locked false)
       (history.h/finalize "Unlock selection"))))

(rf/reg-event-db
 ::set-attr
 (fn [db [_ k v]]
   (-> (h/set-attr db k v)
       (history.h/finalize (str "Set " (name k))))))

(rf/reg-event-db
 ::remove-attr
 (fn [db [_ k]]
   (-> (h/dissoc-attr db k)
       (history.h/finalize (str "Remove " (name k))))))

(rf/reg-event-db
 ::update-attr
 (fn [db [_ k f & more]]
   (-> (reduce (apply partial-right h/update-attr k f more) db (h/selected-ids db))
       (history.h/finalize (str "Update " (name k))))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-fx
 ::fill
 (fn [db [_ color]]
   (-> (h/set-attr db :fill color)
       (history.h/finalize "Fill"))))

(rf/reg-event-db
 ::delete
 (fn [db]
   (-> (h/delete db)
       (history.h/finalize "Delete selection"))))

(rf/reg-event-db
 ::deselect-all
 (fn [db]
   (-> (h/deselect db)
       (history.h/finalize "Deselect all"))))

(rf/reg-event-db
 ::select-all
 (fn [db]
   (-> (h/select-all db)
       (history.h/finalize "Select all"))))

(rf/reg-event-db
 ::select-same-tags
 (fn [db]
   (-> (h/select-same-tags db)
       (history.h/finalize "Select same tags"))))

(rf/reg-event-db
 ::invert-selection
 (fn [db]
   (-> (h/invert-selection db)
       (history.h/finalize "Invert selection"))))

(rf/reg-event-db
 ::raise
 (fn [db]
   (-> (h/update-index db inc)
       (history.h/finalize "Raise selection"))))

(rf/reg-event-db
 ::lower
 (fn [db]
   (-> (h/update-index db dec)
       (history.h/finalize "Lower selection"))))

(rf/reg-event-db
 ::raise-to-top
 (fn [db]
   (-> (h/update-index db (fn [_i sibling-count] (dec sibling-count)))
       (history.h/finalize "Raise selection to top"))))

(rf/reg-event-db
 ::lower-to-bottom
 (fn [db]
   (-> (h/update-index db #(identity 0))
       (history.h/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 ::align
 (fn [db [_ direction]]
   (-> (h/align db direction)
       (history.h/finalize (str "Update " direction)))))

(rf/reg-event-fx
 ::export-svg
 (fn [{:keys [db]} _]
   (let [els (h/root-children db)
         svg (element/->svg els)]
     (if system/electron?
       {::window.fx/ipc-invoke {:channel "export"
                                :data svg
                                :on-error [::notification.e/exception]}}
       {::app.fx/file-save [:data svg
                            :on-error [::notification.e/exception]
                            :options {:startIn "pictures"
                                      :types [{:accept {"image/svg+xml" [".svg"]}}]}]}))))

(rf/reg-event-fx
 ::print
 (fn [{:keys [db]} _]
   (let [els (h/root-children db)
         svg (element/->svg els)]
     (if system/electron?
       {::window.fx/ipc-invoke {:channel "print"
                                :data svg
                                :on-success [::notification.e/add]
                                :on-error [::notification.e/exception]}}
       {::fx/print svg}))))

(rf/reg-event-db
 ::paste
 (fn [db]
   (-> (h/paste db)
       (history.h/finalize "Paste selection"))))

(rf/reg-event-db
 ::paste-in-place
 (fn [db]
   (-> (h/paste-in-place db)
       (history.h/finalize "Paste selection in place"))))

(rf/reg-event-db
 ::paste-styles
 (fn [db]
   (-> (h/paste-styles db)
       (history.h/finalize "Paste styles to selection"))))

(rf/reg-event-db
 ::duplicate
 (fn [db]
   (-> (h/duplicate db)
       (history.h/finalize "Duplicate selection"))))

(rf/reg-event-db
 ::translate
 (fn [db [_ offset]]
   (-> (h/translate db offset)
       (history.h/finalize "Move selection"))))

(rf/reg-event-db
 ::place
 (fn [db [_ position]]
   (-> (h/place db position)
       (history.h/finalize "Place selection"))))

(rf/reg-event-db
 ::scale
 (fn [db [_ ratio]]
   (let [pivot-point (-> db h/bounds bounds/center)]
     (-> (h/scale db ratio pivot-point false)
         (history.h/finalize "Scale selection")))))

(rf/reg-event-db
 ::move-up
 (fn [db _]
   (-> (h/translate db [0 -1])
       (history.h/finalize "Move selection up"))))

(rf/reg-event-db
 ::move-down
 (fn [db _]
   (-> (h/translate db [0 1])
       (history.h/finalize "Move selection down"))))

(rf/reg-event-db
 ::move-left
 (fn [db _]
   (-> (h/translate db [-1 0])
       (history.h/finalize "Move selection left"))))

(rf/reg-event-db
 ::move-right
 (fn [db [_]]
   (-> (h/translate db [1 0])
       (history.h/finalize "Move selection right"))))

(rf/reg-event-db
 ::->path
 (fn [db]
   (-> (h/->path db)
       (history.h/finalize "Convert selection to path"))))

(rf/reg-event-db
 ::stroke->path
 (fn [db]
   (-> (h/stroke->path db)
       (history.h/finalize "Convert selection's stroke to path"))))

(rf/reg-event-db
 ::boolean-operation
 (fn [db [_ operation]]
   (cond-> db
     (seq (rest (h/selected db)))
     (-> (h/boolean-operation operation)
         (history.h/finalize (str/capitalize (name operation)))))))

(rf/reg-event-db
 ::add
 (fn [db [_ el]]
   (-> (h/add db el)
       (history.h/finalize (str "Create " (name (:tag el)))))))

(rf/reg-event-db
 ::import
 (fn [db [_ data]]
   (-> (h/import-svg db data)
       (history.h/finalize "Import svg"))))

(rf/reg-event-db
 ::animate
 (fn [db [_ tag attrs]]
   (-> (h/animate db tag attrs)
       (history.h/finalize (str/capitalize (name tag))))))

(rf/reg-event-db
 ::set-parent
 (fn [db [_ parent-id id]]
   (-> (h/set-parent db parent-id id)
       (history.h/finalize "Set parent"))))

(rf/reg-event-db
 ::group
 (fn [db]
   (-> (h/group db)
       (history.h/finalize "Group selection"))))

(rf/reg-event-db
 ::ungroup
 (fn [db]
   (-> (h/ungroup db)
       (history.h/finalize "Ungroup selection"))))

(rf/reg-event-db
 ::manipulate-path
 (fn [db [_ action]]
   (-> (h/manipulate-path db action)
       (history.h/finalize (str (str/capitalize (name action)) " path")))))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} _]
   (let [els (h/top-selected-sorted db)]
     {:db (h/copy db)
      :fx [(when (seq els)
             [::app.fx/clipboard-write (element/->svg els)])]})))

(rf/reg-event-fx
 ::cut
 (fn [{:keys [db]} _]
   (let [els (h/top-selected-sorted db)]
     {:db (-> (h/copy db)
              (h/delete)
              (history.h/finalize "Cut selection"))
      :fx [(when (seq els)
             [::app.fx/clipboard-write (element/->svg els)])]})))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (h/filter-by-tag db :image)]
     {::fx/trace images})))

(rf/reg-event-db
 ::traced
 (fn [db [_ data]]
   (-> (h/import-svg db data)
       (history.h/finalize "Trace image"))))
