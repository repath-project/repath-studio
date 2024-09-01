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
   [renderer.window.effects :as-alias window.fx]
   [renderer.worker.events :as-alias worker.e]))

(rf/reg-event-fx
 ::select
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ id multi?]]
   {:db (-> db
            (h/select id multi?)
            (history.h/finalize now "Select element"))}))

(rf/reg-event-fx
 ::toggle-prop
 (rf/inject-cofx ::app.fx/now)
 (fn [{:keys [db now]} [_ id k]]
   {:db (-> db
            (h/toggle-prop id k)
            (history.h/finalize now "Toggle " (name k)))}))

(rf/reg-event-db
 ::preview-prop
 (fn [db [_ id k v]]
   (h/assoc-prop db id k v)))

(rf/reg-event-fx
 ::set-prop
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ id k v]]
   {:db (-> db
            (h/assoc-prop id k v)
            (history.h/finalize now "Set " (name k)))}))

(rf/reg-event-fx
 ::lock
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/lock)
            (history.h/finalize now "Lock selection"))}))

(rf/reg-event-fx
 ::unlock
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/unlock)
            (history.h/finalize now "Unlock selection"))}))

(rf/reg-event-fx
 ::set-attr
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ k v]]
   {:db (-> db
            (h/set-attr k v)
            (history.h/finalize now "Set " (name k)))}))

(rf/reg-event-fx
 ::remove-attr
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ k]]
   {:db (-> db
            (h/remove-attr k)
            (history.h/finalize now "Remove " (name k)))}))

(rf/reg-event-fx
 ::update-attr
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ k f & more]]
   {:db (-> (reduce #(apply h/update-attr %1 %2 k f more) db (h/selected-ids db))
            (history.h/finalize now "Update " (name k)))}))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-fx
 ::fill
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ color]]
   {:db (-> db
            (h/set-attr :fill color)
            (history.h/finalize now "Fill color"))}))

(rf/reg-event-fx
 ::delete
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/delete)
            (history.h/finalize now "Delete selection"))}))

(rf/reg-event-fx
 ::deselect-all
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/deselect)
            (history.h/finalize now "Deselect all"))}))

(rf/reg-event-fx
 ::select-all
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/select-all)
            (history.h/finalize now "Select all"))}))

(rf/reg-event-fx
 ::select-same-tags
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/select-same-tags)
            (history.h/finalize now "Select same tags"))}))

(rf/reg-event-fx
 ::invert-selection
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/invert-selection)
            (history.h/finalize now "Invert selection"))}))

(rf/reg-event-fx
 ::raise
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/raise)
            (history.h/finalize now "Raise selection"))}))

(rf/reg-event-fx
 ::lower
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/lower)
            (history.h/finalize now "Lower selection"))}))

(rf/reg-event-fx
 ::raise-to-top
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/raise-to-top)
            (history.h/finalize now "Raise selection to top"))}))

(rf/reg-event-fx
 ::lower-to-bottom
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/lower-to-bottom)
            (history.h/finalize now "Lower selection to bottom"))}))

(rf/reg-event-fx
 ::align
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ direction]]
   {:db (-> db
            (h/align direction)
            (history.h/finalize now "Align " (name direction)))}))

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

(rf/reg-event-fx
 ::paste
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            h/paste
            (history.h/finalize now "Paste selection"))}))

(rf/reg-event-fx
 ::paste-in-place
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/paste-in-place)
            (history.h/finalize now "Paste selection in place"))}))

(rf/reg-event-fx
 ::paste-styles
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} _]
   {:db (-> db
            (h/paste-styles)
            (history.h/finalize now "Paste styles to selection"))}))

(rf/reg-event-fx
 ::duplicate-in-place
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_]]
   {:db (-> db
            (h/duplicate-in-place)
            (history.h/finalize now "Duplicate selection"))}))

(rf/reg-event-fx
 ::translate
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ offset]]
   {:db (-> db
            (h/translate offset)
            (history.h/finalize now "Move selection"))}))

(rf/reg-event-fx
 ::position
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ position]]
   {:db (-> db
            (h/position position)
            (history.h/finalize now "Position selection"))}))

(rf/reg-event-fx
 ::scale
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ ratio]]
   {:db (let [bounds (h/bounds db)
              pivot-point (bounds/center bounds)]
          (-> db
              (h/scale ratio pivot-point true)
              (history.h/finalize now "Scale selection")))}))

(rf/reg-event-fx
 ::move-up
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_]]
   {:db (-> db
            (h/translate [0 -1])
            (history.h/finalize now "Move selection up"))}))

(rf/reg-event-fx
 ::move-down
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_]]
   {:db (-> db
            (h/translate [0 1])
            (history.h/finalize now "Move selection down"))}))

(rf/reg-event-fx
 ::move-left
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_]]
   {:db (-> db
            (h/translate [-1 0])
            (history.h/finalize now "Move selection left"))}))

(rf/reg-event-fx
 ::move-right
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_]]
   {:db (-> db
            (h/translate [1 0])
            (history.h/finalize now "Move selection right"))}))

(rf/reg-event-fx
 ::->path
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]}  _]
   {:db (-> db
            (h/->path)
            (history.h/finalize now "Convert selection to path"))}))

(rf/reg-event-fx
 ::stroke->path
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]}  _]
   {:db (-> db
            (h/stroke->path)
            (history.h/finalize now "Convert selection's stroke to path"))}))

(rf/reg-event-fx
 ::bool-operation
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]}  [_ operation]]
   {:db (if (seq (rest (h/selected db)))
          (-> db
              (h/bool-operation operation)
              (history.h/finalize now (-> operation name str/capitalize))) db)}))

(rf/reg-event-fx
 ::add
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ el]]
   {:db (-> db
            (h/add el)
            (history.h/finalize now "Create " (name (:tag el))))}))

(rf/reg-event-fx
 ::import
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ data msg]]
   {:db (-> db
            (h/import-svg data)
            (assoc :loading? false)
            (history.h/finalize now msg))}))

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
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ tag attrs]]
   {:db (-> db
            (h/animate tag attrs)
            (history.h/finalize now (name tag)))}))

(rf/reg-event-fx
 ::set-parent
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]}  [_ parent-id id]]
   {:db (-> db
            (h/set-parent parent-id id)
            (history.h/finalize now "Set parent"))}))

(rf/reg-event-fx
 ::group
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]}  _]
   {:db (-> db
            (h/group)
            (history.h/finalize now "Group selection"))}))

(rf/reg-event-fx
 ::ungroup
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]}  _]
   {:db (-> db
            h/ungroup
            (history.h/finalize now "Ungroup selection"))}))

(rf/reg-event-fx
 ::manipulate-path
 [(rf/inject-cofx ::app.fx/now)]
 (fn [{:keys [db now]} [_ action]]
   {:db (-> db
            (h/manipulate-path action)
            (history.h/finalize now (str/capitalize (name action)) "path"))}))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} [_]]
   {:db (h/copy db)
    ::app.fx/clipboard-write [(element/->svg (h/top-selected-sorted db))]}))

(rf/reg-event-fx
 ::cut
 (rf/inject-cofx ::app.fx/now)
 (fn [{:keys [db now]} [_]]
   {:db (-> db
            (h/copy)
            (h/delete)
            (history.h/finalize now "Cut selection"))
    ::app.fx/clipboard-write [(element/->svg (h/top-selected-sorted db))]}))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (h/filter-by-tag db :image)]
     {::fx/->svg [images "Tracing" "trace.js"]})))
