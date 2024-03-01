(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
   [renderer.history.views :as history]))

(defn actions []
  [:div.toolbar

   [comp/icon-button
    "file"
    {:title "New"
     :on-click #(rf/dispatch [:document/new])}]

   [comp/icon-button
    "folder"
    {:title "Open"
     :on-click #(rf/dispatch [:document/open])}]

   [comp/icon-button
    "save"
    {:title "Save"
     :on-click #(rf/dispatch [:document/save])
     :disabled @(rf/subscribe [:document/active-saved?])}]

   [:span.v-divider]

   [comp/icon-button
    "undo"
    {:title "Undo"
     :style {:margin-right 0}
     :on-click #(rf/dispatch [:history/undo])
     :disabled (not @(rf/subscribe [:history/undos?]))}]

   [history/select
    "Undo stack"
    @(rf/subscribe [:history/undos])
    (not @(rf/subscribe [:history/undos?]))]

   [comp/icon-button "redo"
    {:title "Undo"
     :style {:margin-right 0}
     :on-click #(rf/dispatch [:history/redo])
     :disabled (not @(rf/subscribe [:history/redos?]))}]

   [history/select
    "Redo stack"
    @(rf/subscribe [:history/redos])
    (not @(rf/subscribe [:history/redos?]))]])

(defn close-button
  [key saved?]
  [:button.close-document-button.small.hover:bg-transparent
   {:key key
    :title "Close document"
    :on-pointer-down #(.stopPropagation %)
    :on-pointer-up (fn [e]
                     (.stopPropagation e)
                     (rf/dispatch [:document/close key]))}
   [comp/icon "times"]
   (when-not saved?
     [:div.circle "●"])])

(defn context-menu
  [key]
  [{:label "Close"
    :action [:document/close key]}
   {:label "Close Others"
    :action [:document/close-others]}
   {:label "Close All"
    :action [:document/close-all]}])

(defn tab
  [key document active?]
  (ra/with-let [dragged-over? (ra/atom false)]
    (let [saved? @(rf/subscribe [:document/saved? key])]
      [:> ContextMenu/Root
       [:> ContextMenu/Trigger
        [:div.document-tab
         {:class [(when active? "active")
                  (when saved? "saved")]
          :on-wheel #(rf/dispatch [:document/scroll (.-deltaY %)])
          :on-pointer-down #(case (.-buttons %)
                              4 (rf/dispatch [:document/close key])
                              1 (rf/dispatch [:set-active-document key])
                              nil)
          :draggable true
          :on-drag-start #(.setData (.-dataTransfer %) "key" (name key))
          :on-drag-over #(.preventDefault %)
          :on-drag-enter #(reset! dragged-over? true)
          :on-drag-leave #(reset! dragged-over? false)
          :on-drop (fn [evt]
                     (.preventDefault evt)
                     (reset! dragged-over? false)
                     (rf/dispatch [:document/swap-position
                                   (-> (.getData (.-dataTransfer evt) "key")
                                       keyword)
                                   key]))}
         [:span.document-name (:title document)]
         [close-button key saved?]]]
       [:> ContextMenu/Portal
        (into
         [:> ContextMenu/Content
          {:class "menu-content context-menu-content"}]
         (map (fn [item]
                [comp/context-menu-item item])
              (context-menu key)))]])))

(defn tab-bar []
  (let [documents @(rf/subscribe [:documents])
        document-tabs @(rf/subscribe [:document-tabs])
        active-document @(rf/subscribe [:active-document])]
    [:div.flex.drag.justify-between {:style {:flex "0 0 40px"}}
     [:div.flex {:style {:flex "1 0 auto"}}
      (for [document document-tabs]
        ^{:key document}
        [tab document (document documents) (= document active-document)])]
     [:div.toolbar
      [:button.p-1.icon-button {:title "Document Actions"}
       [comp/icon "ellipsis-h"]]]]))
