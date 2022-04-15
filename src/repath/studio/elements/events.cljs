(ns repath.studio.elements.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.elements.handlers :as h]
   [repath.studio.history.handlers :as history]
   [repath.studio.tools.base :as tools]))

(rf/reg-event-db
 :elements/select
 (fn [db [_ multiselect element]]
   (h/select db multiselect element)))

(rf/reg-event-db
 :elements/toggle-property
 (fn [db [_ key property]]
   (-> db
       (h/toggle-property key property)
       (history/finalize (str "Toggle " (name property))))))

(rf/reg-event-db
 :elements/set-property
 (fn [db [_ key property value finalize?]]
   (if key (cond-> db
             :always (h/set-property key property value)
             finalize? (history/finalize (str "Set " (name property) " to " value))) db)))

(rf/reg-event-db
 :elements/set-attribute
 (fn [db [_ attribute value finalize?]]
   (cond-> db
       :always (h/set-attribute attribute value)
       finalize? (history/finalize (str "Set " (name attribute) " to " value)))))

(rf/reg-event-db
 :elements/fill
 (fn [db [_ color]]
   (-> db
       (h/set-attribute :fill color)
       (history/finalize (str "Fill " color)))))

(rf/reg-event-db
 :elements/delete
 (fn [db _]
   (-> db
       (h/delete)
       (h/deselect-all)
       (history/finalize "Delete selection"))))

(rf/reg-event-db
 :elements/deselect-all
 (fn [db _]
   (h/deselect-all db)))

(rf/reg-event-db
 :elements/select-all
 (fn [{active-document :active-document :as db} _]
   (let [active-page (get-in db [:documents active-document :active-page])]
     (assoc-in db [:documents active-document :selected-keys] (set (get-in db [:documents active-document :elements active-page :children]))))))

(rf/reg-event-db
 :elements/raise
 (fn [db _]
   (-> db
       (h/update-selected h/raise)
       (history/finalize "Raise selection"))))

(rf/reg-event-db
 :elements/lower
 (fn [db _]
   (-> db
       (h/update-selected h/lower)
       (history/finalize "Lower selection"))))

(rf/reg-event-db
 :elements/raise-to-top
 (fn [db _]
   (-> db
       (h/update-selected h/raise-to-top)
       (history/finalize "Raise selection to top"))))

(rf/reg-event-db
 :elements/lower-to-bottom
 (fn [db _]
   (-> db
       (h/update-selected h/lower-to-bottom)
       (history/finalize "Lower selection to bottom"))))

(rf/reg-event-db
 :elements/align
 (fn [db [_ direction]]
   (-> db
       (h/align direction)
       (history/finalize (str "Align " (name direction))))))

(rf/reg-event-db
 :elements/export
 (fn [{active-document :active-document :as db} _]
   (let [active-page (get-in db [:documents active-document :active-page])
         html (tools/render-to-string (get-in db [:documents active-document :elements active-page]))]
     (js/window.api.send "toMain" #js {:action "export" :data html}))))

(rf/reg-event-db
 :elements/copy
 (fn [db _]
   (h/copy db)))

(rf/reg-event-db
 :elements/cut
 (fn [db _]
   (-> db
       (h/copy)
       (h/delete)
       (history/finalize "Cut selection"))))

(rf/reg-event-db
 :elements/paste
 (fn [db _]
   (-> db
       (h/paste)
       (history/finalize "Paste selection"))))

(rf/reg-event-db
 :elements/paste-in-position
 (fn [db _]
   (-> db
       (h/paste-in-position)
       (history/finalize "Paste selection in position"))))

(rf/reg-event-db
 :elements/paste-styles
 (fn [db _]
   (-> db
       (h/paste-styles)
       (history/finalize "Paste styles to selection"))))

(rf/reg-event-db
 :elements/duplicate
 (fn [db [_]]
   (-> db
       (h/duplicate)
       (history/finalize "Duplicate selection"))))

(rf/reg-event-db
 :elements/translate
 (fn [db [_ offset]]
   (-> db
       (h/translate offset)
       (history/finalize (str "Move selection " offset)))))

(rf/reg-event-db
 :elements/to-path
 (fn [db  _]
   (-> db
       (h/to-path)
       (history/finalize "Convert selection to path"))))

(rf/reg-event-db
 :elements/create
 (fn [db [_ element]]
   (-> db
       (h/create element)
       (history/finalize (str "Create " (name (:type element)))))))

(rf/reg-event-db
 :elements/animate
 (fn [db [_ type attrs]]
   (-> db
       (h/animate type attrs)
       (history/finalize (name type)))))

(rf/reg-event-db
 :elements/set-parent
 (fn [db  [_ element parent-key]]
   (-> db
       (h/set-parent element parent-key)
       (history/finalize (str "Set parent of " (:key element) " to " parent-key)))))

(rf/reg-event-db
 :elements/group
 (fn [db  _]
   (-> db
       (h/group)
       (history/finalize "Group selection"))))

(rf/reg-event-db
 :elements/ungroup
 (fn [db  _]
   (-> db
       (h/ungroup)
       (history/finalize "Ungroup selection"))))
