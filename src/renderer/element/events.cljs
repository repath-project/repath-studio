(ns renderer.element.events
  (:require
   [clojure.string :as str]
   [platform :as platform]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.element.effects :as fx]
   [renderer.element.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.notification.events :as-alias notification.e]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.extra :refer [partial-right]]
   [renderer.window.effects :as-alias window.fx]
   [renderer.worker.events :as-alias worker.e]))

(rf/reg-event-db
 ::select
 [(history.h/finalize "Select element")]
 (fn [db [_ id multi?]]
   (h/select db id multi?)))

(rf/reg-event-db
 ::toggle-prop
 [(history.h/finalize #(str "Toggle " (name (last %))))]
 (fn [db [_ id k]]
   (h/toggle-prop db id k)))

(rf/reg-event-db
 ::preview-prop
 (fn [db [_ id k v]]
   (h/assoc-prop db id k v)))

(rf/reg-event-db
 ::set-prop
 [(history.h/finalize #(str "Set " (name (get % 2))))]
 (fn [db [_ id k v]]
   (h/assoc-prop db id k v)))

(rf/reg-event-db
 ::lock
 [(history.h/finalize "Lock selection")]
 (fn [db]
   (h/lock db)))

(rf/reg-event-db
 ::unlock
 [(history.h/finalize "Unlock selection")]
 (fn [db]
   (h/unlock db)))

(rf/reg-event-db
 ::set-attr
 [(history.h/finalize #(str "Set " (name (second %))))]
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-db
 ::remove-attr
 [(history.h/finalize #(str "Remove " (name (last %))))]
 (fn [db [_ k]]
   (h/remove-attr db k)))

(rf/reg-event-db
 ::update-attr
 [(history.h/finalize #(str "Update " (name (second %))))]
 (fn [db [_ k f & more]]
   (reduce (apply partial-right h/update-attr k f more) db (h/selected-ids db))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-fx
 ::fill
 [(history.h/finalize "Fill")]
 (fn [db [_ color]]
   (h/set-attr db :fill color)))

(rf/reg-event-db
 ::delete
 [(history.h/finalize "Delete selection")]
 (fn [db]
   (h/delete db)))

(rf/reg-event-db
 ::deselect-all
 [(history.h/finalize "Deselect all")]
 (fn [db]
   (h/deselect db)))

(rf/reg-event-db
 ::select-all
 [(history.h/finalize "Select all")]
 h/select-all)

(rf/reg-event-db
 ::select-same-tags
 [(history.h/finalize "Select same tags")]
 h/select-same-tags)

(rf/reg-event-db
 ::invert-selection
 [(history.h/finalize "Invert selection")]
 h/invert-selection)

(rf/reg-event-db
 ::raise
 [(history.h/finalize "Raise selection")]
 (fn [db]
   (h/raise db)))

(rf/reg-event-db
 ::lower
 [(history.h/finalize "Lower selection")]
 (fn [db]
   (h/lower db)))

(rf/reg-event-db
 ::raise-to-top
 [(history.h/finalize "Raise selection to top")]
 (fn [db]
   (h/raise-to-top db)))

(rf/reg-event-db
 ::lower-to-bottom
 [(history.h/finalize "Lower selection to bottom")]
 (fn [db]
   (h/lower-to-bottom db)))

(rf/reg-event-db
 ::align
 [(history.h/finalize #(str "Update " (name (last %))))]
 (fn [db [_ direction]]
   (h/align db direction)))

(rf/reg-event-fx
 ::export-svg
 (fn [{:keys [db]} _]
   (let [els (h/root-children db)
         svg (element/->svg els)]
     (if platform/electron?
       {::window.fx/ipc-invoke {:channel "export"
                                :data svg}}
       {::fx/export [svg {:startIn "pictures"
                          :types [{:accept {"image/svg+xml" [".svg"]}}]}]}))))

(rf/reg-event-fx
 ::print
 (fn [{:keys [db]} _]
   (let [els (h/root-children db)
         svg (element/->svg els)]
     (if platform/electron?
       {::window.fx/ipc-invoke {:channel "print"
                                :data svg
                                :on-resolution ::notification.e/add}}
       {::fx/print svg}))))

(rf/reg-event-db
 ::paste
 [(history.h/finalize "Paste selection")]
 (fn [db]
   (h/paste db)))

(rf/reg-event-db
 ::paste-in-place
 [(history.h/finalize "Paste selection in place")]
 (fn [db]
   (h/paste-in-place db)))

(rf/reg-event-db
 ::paste-styles
 [(history.h/finalize "Paste styles to selection")]
 (fn [db]
   (h/paste-styles db)))

(rf/reg-event-db
 ::duplicate-in-place
 [(history.h/finalize "Duplicate selection")]
 (fn [db]
   (h/duplicate-in-place db)))

(rf/reg-event-db
 ::translate
 [(history.h/finalize "Move selection")]
 (fn [db [_ offset]]
   (h/translate db offset)))

(rf/reg-event-db
 ::position
 [(history.h/finalize "Position selection")]
 (fn [db [_ position]]
   (h/position db position)))

(rf/reg-event-db
 ::scale
 [(history.h/finalize "Scale selection")]
 (fn [db [_ ratio]]
   (let [pivot-point (-> db h/bounds bounds/center)]
     (h/scale db ratio pivot-point true))))

(rf/reg-event-db
 ::move-up
 [(history.h/finalize "Move selection up")]
 (fn [db _]
   (h/translate db [0 -1])))

(rf/reg-event-db
 ::move-down
 [(history.h/finalize "Move selection down")]
 (fn [db _]
   (h/translate db [0 1])))

(rf/reg-event-db
 ::move-left
 [(history.h/finalize "Move selection left")]
 (fn [db _]
   (h/translate db [-1 0])))

(rf/reg-event-db
 ::move-right
 [(history.h/finalize "Move selection right")]
 (fn [db [_]]
   (h/translate db [1 0])))

(rf/reg-event-db
 ::->path
 [(history.h/finalize "Convert selection to path")]
 (fn [db]
   (h/->path db)))

(rf/reg-event-db
 ::stroke->path
 [(history.h/finalize "Convert selection's stroke to path")]
 (fn [db]
   (h/stroke->path db)))

(rf/reg-event-db
 ::bool-operation
 [(history.h/finalize #(-> % last name str/capitalize))]
 (fn [db [_ operation]]
   (cond-> db
     (seq (rest (h/selected db)))
     (h/bool-operation operation))))

(rf/reg-event-db
 ::add
 [(history.h/finalize #(str "Create " (-> % last :tag name)))]
 (fn [db [_ el]]
   (h/add db el)))

(rf/reg-event-db
 ::import
 [(history.h/finalize #(last %))]
 (fn [db [_ data _msg]]
   (-> db
       (h/import-svg data)
       (assoc :loading? false))))

(rf/reg-event-fx
 ::import-svg
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :loading? true)
    :dispatch ^:flush-dom [::import data "Import svg"]}))

(rf/reg-event-fx
 ::import-traced-image
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :loading? true)
    :dispatch ^:flush-dom [::import data "Trace image"]}))

(rf/reg-event-fx
 ::animate
 [(history.h/finalize #(-> % second name))]
 (fn [db [_ tag attrs]]
   (h/animate db tag attrs)))

(rf/reg-event-db
 ::set-parent
 [(history.h/finalize "Set parent")]
 (fn [db [_ parent-id id]]
   (h/set-parent db parent-id id)))

(rf/reg-event-db
 ::group
 [(history.h/finalize "Group selection")]
 h/group)

(rf/reg-event-db
 ::ungroup
 [(history.h/finalize "Ungroup selection")]
 (fn [db]
   (h/ungroup db)))

(rf/reg-event-db
 ::manipulate-path
 [(history.h/finalize #(-> % last name str/capitalize (str " path")))]
 (fn [db [_ action]]
   (h/manipulate-path db action)))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} _]
   {:db (h/copy db)
    ::app.fx/clipboard-write [(element/->svg (h/top-selected-sorted db))]}))

(rf/reg-event-fx
 ::cut
 [(history.h/finalize "Cut selection")]
 (fn [{:keys [db]} _]
   {:db (-> db h/copy h/delete)
    ::app.fx/clipboard-write [(element/->svg (h/top-selected-sorted db))]}))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (h/filter-by-tag db :image)]
     {::fx/->svg [images "Tracing" "trace.js"]})))
