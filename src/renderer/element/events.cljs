(ns renderer.element.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as handlers]
   [renderer.frame.handlers :as frame-handlers]
   [renderer.history.handlers :as history-handlers]
   [renderer.tools.base :as tools]
   [clojure.string :as str]))

(rf/reg-event-db
 :elements/select
 (fn [db [_ multiselect element]]
   (-> db
       (handlers/select multiselect element)
       (history-handlers/finalize "Select element"))))

(rf/reg-event-db
 :elements/toggle-property
 (fn [db [_ key property]]
   (-> db
       (handlers/toggle-property key property)
       (history-handlers/finalize (str "Toggle " (name property))))))

(rf/reg-event-db
 :elements/preview-property
 (fn [db [_ key property value]]
   (handlers/set-property db key property value)))

(rf/reg-event-db
 :elements/set-property
 (fn [db [_ key property value]]
   (-> db
       (handlers/set-property key property value)
       (history-handlers/finalize (str "Set " (name property) " to " value)))))

(rf/reg-event-db
 :elements/set-attribute
 (fn [db [_ attribute value]]
   (-> db
       (handlers/set-attribute attribute value)
       (history-handlers/finalize (str "Set " (name attribute) " to " value)))))

(rf/reg-event-db
 :elements/inc-attribute
 (fn [db [_ attribute]]
   (-> db
       (handlers/update-attribute attribute inc)
       (history-handlers/finalize (str "Increase " (name attribute))))))

(rf/reg-event-db
 :elements/dec-attribute
 (fn [db [_ attribute]]
   (-> db
       (handlers/update-attribute attribute dec)
       (history-handlers/finalize (str "Decrease " (name attribute))))))

(rf/reg-event-db
 :elements/preview-attribute
 (fn [db [_ attribute value]]
   (handlers/set-attribute db attribute value)))

(rf/reg-event-db
 :elements/fill
 (fn [db [_ color]]
   (-> db
       (handlers/set-attribute :fill color)
       (history-handlers/finalize (str "Fill " color)))))

(rf/reg-event-db
 :elements/delete
 (fn [db _]
   (-> db
       (handlers/delete)
       (handlers/deselect-all)
       (history-handlers/finalize "Delete selection"))))

(rf/reg-event-db
 :elements/deselect-all
 (fn [db _]
   (-> db
       (handlers/deselect-all)
       (history-handlers/finalize "Deselect all"))))

(rf/reg-event-db
 :elements/select-all
 (fn [db _]
   (-> db
       (handlers/select-all)
       (history-handlers/finalize "Select all"))))

(rf/reg-event-db
 :elements/select-same-tags
 (fn [db _]
   (-> db
       (handlers/select-same-tags)
       (history-handlers/finalize "Select same tags"))))

(rf/reg-event-db
 :elements/invert-selection
 (fn [db _]
   (-> db
       (handlers/invert-selection)
       (history-handlers/finalize "Invert selection"))))

(rf/reg-event-db
 :elements/raise
 (fn [db _]
   (-> db
       (handlers/update-by handlers/raise)
       (history-handlers/finalize "Raise selection"))))

(rf/reg-event-db
 :elements/lower
 (fn [db _]
   (-> db
       (handlers/update-by handlers/lower)
       (history-handlers/finalize "Lower selection"))))

(rf/reg-event-db
 :elements/raise-to-top
 (fn [db _]
   (-> db
       (handlers/update-by handlers/raise-to-top)
       (history-handlers/finalize "Raise selection to top"))))

(rf/reg-event-db
 :elements/lower-to-bottom
 (fn [db _]
   (-> db
       (handlers/update-by handlers/lower-to-bottom)
       (history-handlers/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 :elements/align
 (fn [db [_ direction]]
   (-> db
       (handlers/align direction)
       (history-handlers/finalize (str "Align " (name direction))))))

(rf/reg-event-db
 :elements/export
 (fn [db _]
   (let [xml (tools/render-to-string (handlers/active-page db))]
     (js/window.api.send "toMain" #js {:action "export" :data xml}))))

(rf/reg-event-db
 :elements/paste
 (fn [db _]
   (-> db
       (handlers/paste)
       (history-handlers/finalize "Paste selection"))))

(rf/reg-event-db
 :elements/paste-in-place
 (fn [db _]
   (-> db
       (handlers/paste-in-place)
       (history-handlers/finalize "Paste selection in place"))))

(rf/reg-event-db
 :elements/paste-styles
 (fn [db _]
   (-> db
       (handlers/paste-styles)
       (history-handlers/finalize "Paste styles to selection"))))

(rf/reg-event-db
 :elements/duplicate-in-place
 (fn [db [_]]
   (-> db
       (handlers/duplicate-in-place)
       (history-handlers/finalize "Duplicate selection"))))

(rf/reg-event-db
 :elements/translate
 (fn [db [_ offset]]
   (-> db
       (handlers/translate offset)
       (history-handlers/finalize (str "Move selection " offset)))))

(rf/reg-event-db
 :elements/move-up
 (fn [db [_]]
   (-> db
       (handlers/translate [0 -1])
       (history-handlers/finalize (str "Move selection up")))))

(rf/reg-event-db
 :elements/move-down
 (fn [db [_]]
   (-> db
       (handlers/translate [0 1])
       (history-handlers/finalize (str "Move selection down")))))

(rf/reg-event-db
 :elements/move-left
 (fn [db [_]]
   (-> db
       (handlers/translate [-1 0])
       (history-handlers/finalize (str "Move selection left")))))

(rf/reg-event-db
 :elements/move-right
 (fn [db [_]]
   (-> db
       (handlers/translate [1 0])
       (history-handlers/finalize (str "Move selection right")))))

(rf/reg-event-db
 :elements/->path
 (fn [db  _]
   (-> db
       (handlers/->path)
       (history-handlers/finalize "Convert selection to path"))))

(rf/reg-event-db
 :elements/stroke->path
 (fn [db  _]
   (-> db
       (handlers/stroke->path)
       (history-handlers/finalize "Convert selection's stroke to path"))))

(rf/reg-event-db
 :elements/bool-operation
 (fn [db  [_ operation]]
   (if (> (count (handlers/selected db)) 1)
     (-> db
         (handlers/bool-operation operation)
         (history-handlers/finalize (-> operation name str/capitalize))) db)))

(rf/reg-event-db
 :elements/create
 (fn [db [_ element]]
   (-> db
       (handlers/create element)
       (history-handlers/finalize (str "Create " (name (:tag element)))))))

(rf/reg-event-db
 :elements/animate
 (fn [db [_ tag attrs]]
   (-> db
       (handlers/animate tag attrs)
       (history-handlers/finalize (name tag)))))

(rf/reg-event-db
 :elements/set-parent
 (fn [db  [_ element parent-key]]
   (-> db
       (handlers/set-parent element parent-key)
       (history-handlers/finalize (str "Set parent of " (:key element) " to " parent-key)))))

(rf/reg-event-db
 :elements/group
 (fn [db  _]
   (-> db
       (handlers/group)
       (history-handlers/finalize "Group selection"))))

(rf/reg-event-db
 :elements/ungroup
 (fn [db  _]
   (-> db
       (handlers/ungroup)
       (history-handlers/finalize "Ungroup selection"))))

(rf/reg-event-db
 :elements/add-page
 (fn [db _]
   (let [[_ y1 x2 _] (tools/elements-bounds
                      (handlers/elements db)
                      (handlers/pages db))
         {:keys [width height fill]} (:attrs (handlers/active-page db))
         db (handlers/create db {:tag :page
                                 :name "Page"
                                 :attrs {:x (+ x2 100)
                                         :y y1
                                         :width width
                                         :height height
                                         :fill fill}})]
     (-> db
         (frame-handlers/pan-to-element (:key (handlers/active-page db)))
         (history-handlers/finalize "Add page")))))

#_:clj-kondo/ignore
(rf/reg-event-db
 :elements/manipulate-path
 (fn [db [_ action]]
   (-> db
       (handlers/manipulate-path action)
       (history-handlers/finalize (str/capitalize (str (name action) "path"))))))