(ns renderer.element.events
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.element.handlers :as h]
   [renderer.frame.handlers :as frame.h]
   [renderer.history.handlers :as history.h]
   [renderer.tools.base :as tools]
   [renderer.utils.bounds :as bounds]))

(rf/reg-event-db
 :element/select
 (fn [db [_ el-k multi?]]
   (-> db
       (h/select el-k multi?)
       (history.h/finalize "Select element"))))

(rf/reg-event-db
 :element/toggle-property
 (fn [db [_ key property]]
   (-> db
       (h/toggle-property key property)
       (history.h/finalize "Toggle " (name property)))))

(rf/reg-event-db
 :element/preview-property
 (fn [db [_ el-k k v]]
   (h/set-property db el-k k v)))

(rf/reg-event-db
 :element/set-property
 (fn [db [_ el-k k v]]
   (-> db
       (h/set-property el-k k v)
       (history.h/finalize "Set " (name k) " to " v))))

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
 :element/set-attribute
 (fn [db [_ k v]]
   (-> db
       (h/set-attribute k v)
       (history.h/finalize "Set " (name k) " to " v))))

(rf/reg-event-db
 :element/inc-attribute
 (fn [db [_ k]]
   (-> db
       (h/update-attribute k inc)
       (history.h/finalize "Increase " (name k)))))

(rf/reg-event-db
 :element/dec-attribute
 (fn [db [_ k]]
   (-> db
       (h/update-attribute k dec)
       (history.h/finalize "Decrease " (name k)))))

(rf/reg-event-db
 :element/preview-attribute
 (fn [db [_ k v]]
   (h/set-attribute db k v)))

(rf/reg-event-db
 :element/fill
 (fn [db [_ color]]
   (-> db
       (h/set-attribute :fill color)
       (history.h/finalize "Fill " color))))

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

(rf/reg-event-db
 :element/export
 (fn [db _]
   (let [xml (tools/render-to-string (h/active-page db))]
     (js/window.api.send "toMain" #js {:action "export" :data xml}))))

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
       (history.h/finalize "Move selection by " offset))))

(rf/reg-event-db
 :element/scale
 (fn [db [_ ratio]]
   (let [bounds (h/bounds db)
         pivot-point (bounds/center bounds)]
     (-> db
         (h/scale ratio pivot-point)
         (history.h/finalize "Scale selection by " ratio)))))

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
   (if (> (count (h/selected db)) 1)
     (-> db
         (h/bool-operation operation)
         (history.h/finalize (-> operation name str/capitalize))) db)))

(rf/reg-event-db
 :element/create
 (fn [db [_ element]]
   (-> db
       (h/create element)
       (history.h/finalize "Create " (name (:tag element))))))

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

(rf/reg-event-db
 :element/add-page
 (fn [db _]
   (let [[_ y1 x2 _] (tools/elements-bounds (h/elements db)
                                            (h/pages db))
         {:keys [width height fill]} (:attrs (h/active-page db))
         db (h/create db {:tag :page
                          :name "Page"
                          :attrs {:x (+ x2 100)
                                  :y y1
                                  :width width
                                  :height height
                                  :fill fill}})]
     (-> db
         (frame.h/pan-to-element (:key (h/active-page db)))
         (history.h/finalize "Add page")))))

#_:clj-kondo/ignore
(rf/reg-event-db
 :element/manipulate-path
 (fn [db [_ action]]
   (-> db
       (h/manipulate-path action)
       (history.h/finalize (str/capitalize (name action)) "path"))))

(defn elements->string
  [elements]
  (reduce #(str % (tools/render-to-string %2)) "" elements))

(rf/reg-event-fx
 :element/copy
 (fn [{:keys [db]} [_]]
   (let [selected-elements (h/selected db)
         text-html (elements->string selected-elements)]
     {:db (h/copy db)
      :clipboard-write [text-html]})))

(rf/reg-event-fx
 :element/cut
 (fn [{:keys [db]} [_]]
   (let [selected-elements (h/selected db)
         text-html (elements->string selected-elements)]
     {:db (-> db
              h/copy
              h/delete
              (history.h/finalize "Cut selection"))
      :clipboard-write [text-html]})))
