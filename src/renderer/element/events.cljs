(ns renderer.element.events
  (:require
   ["imagetracerjs" :as ImageTracer]
   ["triangulate-image" :as triangulate]
   [clojure.string :as str]
   [platform]
   [promesa.core :as p]
   [re-frame.core :as rf]
   [renderer.element.handlers :as h]
   [renderer.history.handlers :as history.h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.file :as file]
   [renderer.utils.units :as units]))

(rf/reg-event-db
 :element/select
 (fn [db [_ el-k multi?]]
   (-> db
       (h/select el-k multi?)
       (history.h/finalize "Select element"))))

(rf/reg-event-db
 :element/select-up
 (fn [db [_ multi?]]
   (-> db
       (h/select-up multi?)
       (history.h/finalize "Select up"))))

(rf/reg-event-db
 :element/select-down
 (fn [db [_ multi?]]
   (-> db
       (h/select-down multi?)
       (history.h/finalize "Select down"))))

(rf/reg-event-db
 :element/toggle-prop
 (fn [db [_ key prop]]
   (-> db
       (h/toggle-prop key prop)
       (history.h/finalize "Toggle " (name prop)))))

(rf/reg-event-db
 :element/preview-prop
 (fn [db [_ el-k k v]]
   (h/set-prop db el-k k v)))

(rf/reg-event-db
 :element/set-prop
 (fn [db [_ el-k k v]]
   (-> db
       (h/set-prop el-k k v)
       (history.h/finalize "Set " (name k)))))

(rf/reg-event-db
 :element/lock
 (fn [db _]
   (-> db
       h/lock
       (history.h/finalize "Lock selection"))))

(rf/reg-event-db
 :element/unlock
 (fn [db _]
   (-> db
       h/unlock
       (history.h/finalize "Unlock selection"))))

(rf/reg-event-db
 :element/set-attr
 (fn [db [_ k v]]
   (-> db
       (h/set-attr k v)
       (history.h/finalize "Set " (name k)))))

(rf/reg-event-db
 :element/remove-attr
 (fn [db [_ k]]
   (-> db
       (h/remove-attr k)
       (history.h/finalize "Remove " (name k)))))

(rf/reg-event-db
 :element/update-attr
 (fn [db [_ k f & more]]
   (-> (reduce #(apply h/update-attr %1 %2 k f more) db (h/selected db))
       (history.h/finalize "Update " (name k)))))

(rf/reg-event-db
 :element/preview-attr
 (fn [db [_ k v]]
   (h/set-attr db k v)))

(rf/reg-event-db
 :element/fill
 (fn [db [_ color]]
   (-> db
       (h/set-attr :fill color)
       (history.h/finalize "Fill color"))))

(rf/reg-event-db
 :element/delete
 (fn [db _]
   (-> db
       h/delete
       (history.h/finalize "Delete selection"))))

(rf/reg-event-db
 :element/deselect-all
 (fn [db _]
   (-> db h/deselect (history.h/finalize "Deselect all"))))

(rf/reg-event-db
 :element/select-all
 (fn [db _]
   (-> db h/select-all (history.h/finalize "Select all"))))

(rf/reg-event-db
 :element/select-same-tags
 (fn [db _]
   (-> db
       h/select-same-tags
       (history.h/finalize "Select same tags"))))

(rf/reg-event-db
 :element/invert-selection
 (fn [db _]
   (-> db
       h/invert-selection
       (history.h/finalize "Invert selection"))))

(rf/reg-event-db
 :element/raise
 (fn [db _]
   (-> db
       h/raise
       (history.h/finalize "Raise selection"))))

(rf/reg-event-db
 :element/lower
 (fn [db _]
   (-> db
       h/lower
       (history.h/finalize "Lower selection"))))

(rf/reg-event-db
 :element/raise-to-top
 (fn [db _]
   (-> db
       h/raise-to-top
       (history.h/finalize "Raise selection to top"))))

(rf/reg-event-db
 :element/lower-to-bottom
 (fn [db _]
   (-> db
       h/lower-to-bottom
       (history.h/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 :element/align
 (fn [db [_ direction]]
   (-> db
       (h/align direction)
       (history.h/finalize "Align " (name direction)))))

(rf/reg-fx
 ::export
 (fn [data]
   (file/save!
    {:startIn "pictures"
     :types [{:accept {"image/svg+xml" [".svg" ".svgo"]}}]}
    (fn [^js/FileSystemFileHandle file-handle]
      (p/let [writable (.createWritable file-handle)]
        (.then (.write writable data) (.close writable)))))))

(rf/reg-event-fx
 :element/export
 (fn [{:keys [db]} _]
   (let [xml (-> db
                 h/root-children
                 h/->string)]
     (if platform/electron?
       {:send-to-main {:action "export" :data xml}}
       {::export xml}))))

(rf/reg-event-db
 :element/paste
 (fn [db _]
   (-> db
       h/paste
       (history.h/finalize "Paste selection"))))

(rf/reg-event-db
 :element/paste-in-place
 (fn [db _]
   (-> db
       h/paste-in-place
       (history.h/finalize "Paste selection in place"))))

(rf/reg-event-db
 :element/paste-styles
 (fn [db _]
   (-> db
       h/paste-styles
       (history.h/finalize "Paste styles to selection"))))

(rf/reg-event-db
 :element/duplicate-in-place
 (fn [db [_]]
   (-> db
       h/duplicate-in-place
       (history.h/finalize "Duplicate selection"))))

(rf/reg-event-db
 :element/translate
 (fn [db [_ offset]]
   (-> db
       (h/translate offset)
       (history.h/finalize "Move selection"))))

(rf/reg-event-db
 :element/position
 (fn [db [_ position]]
   (-> db
       (h/position position)
       (history.h/finalize "Position selection"))))

(rf/reg-event-db
 :element/scale
 (fn [db [_ ratio]]
   (let [bounds (h/bounds db)
         pivot-point (bounds/center bounds)]
     (-> db
         (h/scale ratio pivot-point)
         (history.h/finalize "Scale selection")))))

(rf/reg-event-db
 :element/move-up
 (fn [db [_]]
   (-> db
       (h/translate [0 -1])
       (history.h/finalize "Move selection up"))))

(rf/reg-event-db
 :element/move-down
 (fn [db [_]]
   (-> db
       (h/translate [0 1])
       (history.h/finalize "Move selection down"))))

(rf/reg-event-db
 :element/move-left
 (fn [db [_]]
   (-> db
       (h/translate [-1 0])
       (history.h/finalize "Move selection left"))))

(rf/reg-event-db
 :element/move-right
 (fn [db [_]]
   (-> db
       (h/translate [1 0])
       (history.h/finalize "Move selection right"))))

(rf/reg-event-db
 :element/->path
 (fn [db  _]
   (-> db
       h/->path
       (history.h/finalize "Convert selection to path"))))

(rf/reg-event-db
 :element/stroke->path
 (fn [db  _]
   (-> db
       h/stroke->path
       (history.h/finalize "Convert selection's stroke to path"))))

(rf/reg-event-db
 :element/bool-operation
 (fn [db  [_ operation]]
   (if (seq (rest (h/selected db)))
     (-> db
         (h/bool-operation operation)
         (history.h/finalize (-> operation name str/capitalize))) db)))

(rf/reg-event-db
 :element/add
 (fn [db [_ el]]
   (-> db
       (h/add el)
       (history.h/finalize "Create " (name (:tag el))))))

(rf/reg-event-db
 :element/import-svg
 (fn [db [_ s name position]]
   (-> db
       (h/import-svg s name position)
       (history.h/finalize "Import svg"))))

(rf/reg-event-db
 :element/import-traced-image
 (fn [db [_ s name position]]
   (-> db
       (h/import-svg s (str "Traced " name) position)
       (history.h/finalize "Trace"))))

(rf/reg-event-db
 :element/animate
 (fn [db [_ tag attrs]]
   (-> db
       (h/animate tag attrs)
       (history.h/finalize (name tag)))))

(rf/reg-event-db
 :element/set-parent
 (fn [db  [_ element-key parent-key]]
   (-> db
       (h/set-parent element-key parent-key)
       (history.h/finalize "Set parent of selection"))))

(rf/reg-event-db
 :element/group
 (fn [db  _]
   (-> db
       h/group
       (history.h/finalize "Group selection"))))

(rf/reg-event-db
 :element/ungroup
 (fn [db  _]
   (-> db
       h/ungroup
       (history.h/finalize "Ungroup selection"))))

#_:clj-kondo/ignore
(rf/reg-event-db
 :element/manipulate-path
 (fn [db [_ action]]
   (-> db
       (h/manipulate-path action)
       (history.h/finalize (str/capitalize (name action)) "path"))))

(rf/reg-event-fx
 :element/copy
 (fn [{:keys [db]} [_]]
   (let [selected-elements (h/selected db)
         text-html (h/->string selected-elements)]
     {:db (h/copy db)
      :clipboard-write [text-html]})))

(rf/reg-event-fx
 :element/cut
 (fn [{:keys [db]} [_]]
   (let [selected-elements (h/selected db)
         text-html (h/->string selected-elements)]
     {:db (-> db
              h/copy
              h/delete
              (history.h/finalize "Cut selection"))
      :clipboard-write [text-html]})))

(rf/reg-fx
 ::->svg
 (fn [[elements f]]
   (doseq [el elements]
     (let [data-url (-> el :attrs :href)
           position (:bounds el)
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
                  (p/let [image-data (.getImageData context 0 0 width height)
                          svg (f image-data)]
                    (rf/dispatch-sync [:element/import-traced-image svg (:name el) position]))))
       (set! (.-src image) data-url)))))

(rf/reg-event-fx
 :element/trace
 (fn [{:keys [db]} [_]]
   (let [images (h/filter-by-tag db :image)]
     {:db (cond-> db images (assoc :loading "Tracing.."))
      :fx [[::->svg [(filter #(= :image (:tag %)) (h/selected db))
                     #(.imagedataToSVG ImageTracer %)]]
           [:dispatch [:clear-loading]]]})))

(def triangulation-options
  #js {:accuracy 0.5 ; float between 0 and 1
       :blur 40 ; positive integer
       :threshold 50 ; integer between 1 and 100
       :vertexCount 100 ; positive integer
       :fill true ; boolean or string with css color (e.g '#bada55', 'red', rgba(100,100,100,0.5))
       :stroke false ; boolean or string with css color (e.g '#bada55', 'red', hsla(0, 50%, 52%, 0.5))
       :strokeWidth 0.5 ; positive float
       :gradients false ; boolean
       :gradientStops 4 ; positive integer >= 2
       :lineJoin "miter" ; 'miter', 'round', or 'bevel'
       :transparentColor false ; boolean false or string with css color
       })

(rf/reg-event-fx
 :element/triangulate
 (fn [{:keys [db]} [_]]
   (let [images (h/filter-by-tag db :image)]
     {:db (cond-> db images (assoc :loading "Triangulating.."))
      :fx [[::->svg [(filter #(= :image (:tag %)) (h/selected db))
                     #(-> triangulation-options
                          (triangulate)
                          (.fromImageDataSync %)
                          (.toSVG))]]
           [:dispatch [:clear-loading]]]})))
