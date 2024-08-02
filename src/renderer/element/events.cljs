(ns renderer.element.events
  (:require
   [clojure.string :as str]
   [platform]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [renderer.element.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.file :as file]
   [renderer.utils.units :as units]
   [renderer.worker.events :as-alias worker.e]))

(rf/reg-event-db
 ::select
 (fn [db [_ el-k multi?]]
   (-> db
       (h/select el-k multi?)
       (history.h/finalize "Select element"))))

(rf/reg-event-db
 ::toggle-prop
 (fn [db [_ key prop]]
   (-> db
       (h/toggle-prop key prop)
       (history.h/finalize "Toggle " (name prop)))))

(rf/reg-event-db
 ::preview-prop
 (fn [db [_ el-k k v]]
   (h/set-prop db el-k k v)))

(rf/reg-event-db
 ::set-prop
 (fn [db [_ el-k k v]]
   (-> db
       (h/set-prop el-k k v)
       (history.h/finalize "Set " (name k)))))

(rf/reg-event-db
 ::lock
 (fn [db _]
   (-> db
       h/lock
       (history.h/finalize "Lock selection"))))

(rf/reg-event-db
 ::unlock
 (fn [db _]
   (-> db
       h/unlock
       (history.h/finalize "Unlock selection"))))

(rf/reg-event-db
 ::set-attr
 (fn [db [_ k v]]
   (-> db
       (h/set-attr k v)
       (history.h/finalize "Set " (name k)))))

(rf/reg-event-db
 ::remove-attr
 (fn [db [_ k]]
   (-> db
       (h/remove-attr k)
       (history.h/finalize "Remove " (name k)))))

(rf/reg-event-db
 ::update-attr
 (fn [db [_ k f & more]]
   (-> (reduce #(apply h/update-attr %1 %2 k f more) db (h/selected-keys db))
       (history.h/finalize "Update " (name k)))))

(rf/reg-event-db
 ::preview-attr
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-db
 ::fill
 (fn [db [_ color]]
   (-> db
       (h/set-attr :fill color)
       (history.h/finalize "Fill color"))))

(rf/reg-event-db
 ::delete
 (fn [db _]
   (-> db
       h/delete
       (history.h/finalize "Delete selection"))))

(rf/reg-event-db
 ::deselect-all
 (fn [db _]
   (-> db h/deselect (history.h/finalize "Deselect all"))))

(rf/reg-event-db
 ::select-all
 (fn [db _]
   (-> db h/select-all (history.h/finalize "Select all"))))

(rf/reg-event-db
 ::select-same-tags
 (fn [db _]
   (-> db
       h/select-same-tags
       (history.h/finalize "Select same tags"))))

(rf/reg-event-db
 ::invert-selection
 (fn [db _]
   (-> db
       h/invert-selection
       (history.h/finalize "Invert selection"))))

(rf/reg-event-db
 ::raise
 (fn [db _]
   (-> db
       h/raise
       (history.h/finalize "Raise selection"))))

(rf/reg-event-db
 ::lower
 (fn [db _]
   (-> db
       h/lower
       (history.h/finalize "Lower selection"))))

(rf/reg-event-db
 ::raise-to-top
 (fn [db _]
   (-> db
       h/raise-to-top
       (history.h/finalize "Raise selection to top"))))

(rf/reg-event-db
 ::lower-to-bottom
 (fn [db _]
   (-> db
       h/lower-to-bottom
       (history.h/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 ::align
 (fn [db [_ direction]]
   (-> db
       (h/align direction)
       (history.h/finalize "Align " (name direction)))))

(rf/reg-fx
 ::export
 (fn [data options]
   (file/save!
    options
    (fn [^js/FileSystemFileHandle file-handle]
      (p/let [writable (.createWritable file-handle)]
        (.then (.write writable data) (.close writable)))))))

(rf/reg-event-fx
 ::export-svg
 (fn [{:keys [db]} _]
   (let [xml (-> db
                 h/root-children
                 h/->string)]
     (if platform/electron?
       {:send-to-main {:action "export" :data xml}}
       {::export [xml {:startIn "pictures"
                       :types [{:accept {"image/svg+xml" [".svg"]}}]}]}))))

(rf/reg-event-db
 ::paste
 (fn [db _]
   (-> db
       h/paste
       (history.h/finalize "Paste selection"))))

(rf/reg-event-db
 ::paste-in-place
 (fn [db _]
   (-> db
       h/paste-in-place
       (history.h/finalize "Paste selection in place"))))

(rf/reg-event-db
 ::paste-styles
 (fn [db _]
   (-> db
       h/paste-styles
       (history.h/finalize "Paste styles to selection"))))

(rf/reg-event-db
 ::duplicate-in-place
 (fn [db [_]]
   (-> db
       h/duplicate-in-place
       (history.h/finalize "Duplicate selection"))))

(rf/reg-event-db
 ::translate
 (fn [db [_ offset]]
   (-> db
       (h/translate offset)
       (history.h/finalize "Move selection"))))

(rf/reg-event-db
 ::position
 (fn [db [_ position]]
   (-> db
       (h/position position)
       (history.h/finalize "Position selection"))))

(rf/reg-event-db
 ::scale
 (fn [db [_ ratio]]
   (let [bounds (h/bounds db)
         pivot-point (bounds/center bounds)]
     (-> db
         (h/scale ratio pivot-point)
         (history.h/finalize "Scale selection")))))

(rf/reg-event-db
 ::move-up
 (fn [db [_]]
   (-> db
       (h/translate [0 -1])
       (history.h/finalize "Move selection up"))))

(rf/reg-event-db
 ::move-down
 (fn [db [_]]
   (-> db
       (h/translate [0 1])
       (history.h/finalize "Move selection down"))))

(rf/reg-event-db
 ::move-left
 (fn [db [_]]
   (-> db
       (h/translate [-1 0])
       (history.h/finalize "Move selection left"))))

(rf/reg-event-db
 ::move-right
 (fn [db [_]]
   (-> db
       (h/translate [1 0])
       (history.h/finalize "Move selection right"))))

(rf/reg-event-db
 ::->path
 (fn [db  _]
   (-> db
       h/->path
       (history.h/finalize "Convert selection to path"))))

(rf/reg-event-db
 ::stroke->path
 (fn [db  _]
   (-> db
       h/stroke->path
       (history.h/finalize "Convert selection's stroke to path"))))

(rf/reg-event-db
 ::bool-operation
 (fn [db  [_ operation]]
   (if (seq (rest (h/selected db)))
     (-> db
         (h/bool-operation operation)
         (history.h/finalize (-> operation name str/capitalize))) db)))

(rf/reg-event-db
 ::add
 (fn [db [_ el]]
   (-> db
       (h/add el)
       (history.h/finalize "Create " (name (:tag el))))))

(rf/reg-event-db
 ::import-svg
 (fn [db [_ data]]
   (-> db
       (h/import-svg data)
       (history.h/finalize "Import svg"))))

(rf/reg-event-db
 ::import-traced-image
 (fn [db [_ data]]
   (-> db
       (h/import-svg data)
       (history.h/finalize "Trace image"))))

(rf/reg-event-db
 ::animate
 (fn [db [_ tag attrs]]
   (-> db
       (h/animate tag attrs)
       (history.h/finalize (name tag)))))

(rf/reg-event-db
 ::set-parent
 (fn [db  [_ element-key parent-key]]
   (-> db
       (h/set-parent element-key parent-key)
       (history.h/finalize "Set parent of selection"))))

(rf/reg-event-db
 ::group
 (fn [db  _]
   (-> db
       h/group
       (history.h/finalize "Group selection"))))

(rf/reg-event-db
 ::ungroup
 (fn [db  _]
   (-> db
       h/ungroup
       (history.h/finalize "Ungroup selection"))))

(rf/reg-event-db
 ::manipulate-path
 (fn [db [_ action]]
   (-> db
       (h/manipulate-path action)
       (history.h/finalize (str/capitalize (name action)) "path"))))

(defn clipboard-data
  [db]
  (let [selected-elements (h/top-selected-sorted db)
        dimensions (bounds/->dimensions (h/bounds db))
        s (h/->string selected-elements)]
    (cond-> s
      (not (and (h/single? selected-elements)
                (or (element/svg? (first selected-elements))
                    (element/root? (first selected-elements)))))
      (element/wrap-to-svg dimensions))))

(rf/reg-event-fx
 ::copy
 (fn [{:keys [db]} [_]]
   {:db (h/copy db)
    :clipboard-write [(clipboard-data db)]}))

(rf/reg-event-fx
 ::cut
 (fn [{:keys [db]} [_]]
   {:db (-> db
            h/copy
            h/delete
            (history.h/finalize "Cut selection"))
    :clipboard-write [(clipboard-data db)]}))

(rf/reg-fx
 ::->svg
 (fn [[elements action worker]]
   (doseq [el elements]
     (let [data-url (-> el :attrs :href)
           [x y] (:bounds el)
           canvas (js/document.createElement "canvas")
           context (.getContext canvas "2d")
           image (js/Image.)
           ;; TODO: Handle preserveAspectRatio.
           width (units/unit->px (-> el :attrs :width))
           height (units/unit->px (-> el :attrs :height))]
       (set! (.-onload image)
             #(do (set! (.-width canvas) width)
                  (set! (.-height canvas) height)
                  (.drawImage context image 0 0 width height)
                  (p/let [image-data (.getImageData context 0 0 width height)]
                    (rf/dispatch [::worker.e/create
                                  {:action action
                                   :worker worker
                                   :data {:name (:name el)
                                          :image image-data
                                          :position [x y]}
                                   :callback (fn [e]
                                               (let [data (js->clj (.. e -data) :keywordize-keys true)]
                                                 (rf/dispatch [::import-traced-image data])
                                                 (rf/dispatch [::worker.e/completed (keyword (:id data))])))}]))))
       (set! (.-src image) data-url)))))

(rf/reg-event-fx
 ::trace
 (fn [{:keys [db]} [_]]
   (let [images (h/filter-by-tag db :image)]
     {::->svg [images "Tracing" "trace.js"]})))
