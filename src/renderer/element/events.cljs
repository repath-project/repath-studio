(ns renderer.element.events
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx :refer [persist]]
   [renderer.element.effects :as-alias fx]
   [renderer.element.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.snap.handlers :as snap.h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.utils.system :as system]
   [renderer.window.effects :as-alias window.fx]))

(rf/reg-event-db
 ::select
 [persist]
 (fn [db [_ id multiple]]
   (-> (h/select db id multiple)
       (snap.h/update-tree)
       (history.h/finalize "Select element"))))

(rf/reg-event-db
 ::select-ids
 [persist]
 (fn [db [_ ids]]
   (-> (reduce (partial-right h/assoc-prop :selected true) (h/deselect db) ids)
       (snap.h/update-tree)
       (history.h/finalize "Select elements"))))

(rf/reg-event-db
 ::toggle-prop
 [persist]
 (fn [db [_ id k]]
   (-> (h/update-prop db id k not)
       (history.h/finalize (str "Toggle " (name k))))))

(rf/reg-event-db
 ::preview-prop
 (fn [db [_ id k v]]
   (h/assoc-prop db id k v)))

(rf/reg-event-db
 ::set-prop
 [persist]
 (fn [db [_ id k v]]
   (-> (h/assoc-prop db id k v)
       (history.h/finalize (str "Set " (name k))))))

(rf/reg-event-db
 ::lock
 [persist]
 (fn [db]
   (-> (h/assoc-prop db :locked true)
       (history.h/finalize "Lock selection"))))

(rf/reg-event-db
 ::unlock
 [persist]
 (fn [db]
   (-> (h/assoc-prop db :locked false)
       (history.h/finalize "Unlock selection"))))

(rf/reg-event-db
 ::set-attr
 [persist]
 (fn [db [_ k v]]
   (-> (h/set-attr db k v)
       (history.h/finalize (str "Set " (name k))))))

(rf/reg-event-db
 ::remove-attr
 [persist]
 (fn [db [_ k]]
   (-> (h/dissoc-attr db k)
       (history.h/finalize (str "Remove " (name k))))))

(rf/reg-event-db
 ::update-attr
 [persist]
 (fn [db [_ k f & more]]
   (-> (reduce (apply partial-right h/update-attr k f more) db (h/selected-ids db))
       (history.h/finalize (str "Update " (name k))))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-fx
 ::fill
 [persist]
 (fn [db [_ color]]
   (-> (h/set-attr db :fill color)
       (history.h/finalize "Fill"))))

(rf/reg-event-db
 ::delete
 [persist]
 (fn [db]
   (-> (h/delete db)
       (history.h/finalize "Delete selection"))))

(rf/reg-event-db
 ::deselect-all
 [persist]
 (fn [db]
   (-> (h/deselect db)
       (history.h/finalize "Deselect all"))))

(rf/reg-event-db
 ::select-all
 [persist]
 (fn [db]
   (-> (h/select-all db)
       (history.h/finalize "Select all"))))

(rf/reg-event-db
 ::select-same-tags
 [persist]
 (fn [db]
   (-> (h/select-same-tags db)
       (history.h/finalize "Select same tags"))))

(rf/reg-event-db
 ::invert-selection
 [persist]
 (fn [db]
   (-> (h/invert-selection db)
       (history.h/finalize "Invert selection"))))

(rf/reg-event-db
 ::raise
 [persist]
 (fn [db]
   (-> (h/update-index db inc)
       (history.h/finalize "Raise selection"))))

(rf/reg-event-db
 ::lower
 [persist]
 (fn [db]
   (-> (h/update-index db dec)
       (history.h/finalize "Lower selection"))))

(rf/reg-event-db
 ::raise-to-top
 [persist]
 (fn [db]
   (-> (h/update-index db (fn [_i sibling-count] (dec sibling-count)))
       (history.h/finalize "Raise selection to top"))))

(rf/reg-event-db
 ::lower-to-bottom
 [persist]
 (fn [db]
   (-> (h/update-index db #(identity 0))
       (history.h/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 ::align
 [persist]
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
                                :data svg}}
       {::app.fx/save [:data svg
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
                                :on-resolution ::notification.e/add}}
       {::fx/print svg}))))

(rf/reg-event-db
 ::paste
 [persist]
 (fn [db]
   (-> (h/paste db)
       (history.h/finalize "Paste selection"))))

(rf/reg-event-db
 ::paste-in-place
 [persist]
 (fn [db]
   (-> (h/paste-in-place db)
       (history.h/finalize "Paste selection in place"))))

(rf/reg-event-db
 ::paste-styles
 [persist]
 (fn [db]
   (-> (h/paste-styles db)
       (history.h/finalize "Paste styles to selection"))))

(rf/reg-event-db
 ::duplicate
 [persist]
 (fn [db]
   (-> (h/duplicate db)
       (history.h/finalize "Duplicate selection"))))

(rf/reg-event-db
 ::translate
 [persist]
 (fn [db [_ offset]]
   (-> (h/translate db offset)
       (history.h/finalize "Move selection"))))

(rf/reg-event-db
 ::place
 [persist]
 (fn [db [_ position]]
   (-> (h/place db position)
       (history.h/finalize "Place selection"))))

(rf/reg-event-db
 ::scale
 [persist]
 (fn [db [_ ratio]]
   (let [pivot-point (-> db h/bounds bounds/center)]
     (-> (h/scale db ratio pivot-point false)
         (history.h/finalize "Scale selection")))))

(rf/reg-event-db
 ::move-up
 [persist]
 (fn [db _]
   (-> (h/translate db [0 -1])
       (history.h/finalize "Move selection up"))))

(rf/reg-event-db
 ::move-down
 [persist]
 (fn [db _]
   (-> (h/translate db [0 1])
       (history.h/finalize "Move selection down"))))

(rf/reg-event-db
 ::move-left
 [persist]
 (fn [db _]
   (-> (h/translate db [-1 0])
       (history.h/finalize "Move selection left"))))

(rf/reg-event-db
 ::move-right
 [persist]
 (fn [db [_]]
   (-> (h/translate db [1 0])
       (history.h/finalize "Move selection right"))))

(rf/reg-event-db
 ::->path
 [persist]
 (fn [db]
   (-> (h/->path db)
       (history.h/finalize "Convert selection to path"))))

(rf/reg-event-db
 ::stroke->path
 [persist]
 (fn [db]
   (-> (h/stroke->path db)
       (history.h/finalize "Convert selection's stroke to path"))))

(rf/reg-event-db
 ::boolean-operation
 [persist]
 (fn [db [_ operation]]
   (cond-> db
     (seq (rest (h/selected db)))
     (-> (h/boolean-operation operation)
         (history.h/finalize (str/capitalize (name operation)))))))

(rf/reg-event-db
 ::add
 [persist]
 (fn [db [_ el]]
   (-> (h/add db el)
       (history.h/finalize (str "Create " (name (:tag el)))))))

(rf/reg-event-db
 ::import
 [persist]
 (fn [db [_ data]]
   (-> (h/import-svg db data)
       (history.h/finalize "Import svg"))))

(rf/reg-event-db
 ::animate
 [persist]
 (fn [db [_ tag attrs]]
   (-> (h/animate db tag attrs)
       (history.h/finalize (comp str/capitalize name second)))))

(rf/reg-event-db
 ::set-parent
 [persist]
 (fn [db [_ parent-id id]]
   (-> (h/set-parent db parent-id id)
       (history.h/finalize "Set parent"))))

(rf/reg-event-db
 ::group
 [persist]
 (fn [db]
   (-> (h/group db)
       (history.h/finalize "Group selection"))))

(rf/reg-event-db
 ::ungroup
 [persist]
 (fn [db]
   (-> (h/ungroup db)
       (history.h/finalize "Ungroup selection"))))

(rf/reg-event-db
 ::manipulate-path
 [persist]
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
 [persist]
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
 [persist]
 (fn [db [_ data]]
   (-> (h/import-svg db data)
       (history.h/finalize "Trace image"))))
