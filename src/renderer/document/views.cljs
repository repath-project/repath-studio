(ns renderer.document.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
   [renderer.history.views :as history]
   ["@radix-ui/react-context-menu" :as ContextMenu]))

(defn actions []
  [:div.flex.toolbar {:style {:overflow "visible"
                              :flex "0 0 40px"}}
   [comp/icon-button {:title "New"
                      :icon "file"
                      :action #(rf/dispatch [:document/new])}]

   [comp/icon-button {:title "Open"
                      :icon "folder"
                      :action #(rf/dispatch [:document/open])}]

   [comp/icon-button {:title "Save"
                      :icon "save"
                      :action #(rf/dispatch [:document/save])}]

   [:span.v-divider]

   [comp/icon-button {:title "Import"
                      :icon "import"
                      :class "disabled"
                      :action #(rf/dispatch [:document/import])}]

   [comp/icon-button {:title "Export"
                      :icon "export"
                      :action #(rf/dispatch [:elements/export])}]

   [:span.v-divider]

   [comp/icon-button {:title "Undo"
                      :icon "undo"
                      :action #(rf/dispatch [:history/undo 1])
                      :disabled? (not @(rf/subscribe [:history/undos?]))}]

   [:select.icon-button
    {:onChange #(rf/dispatch [:history/undo (-> % .-target .-value js/parseInt)])
     :disabled (not @(rf/subscribe [:history/undos?]))
     :style {:margin-left "-2px"
             :max-width "14px"
             :background "var(--level-0)"
             :font-size "1em"}}
    (history/select-options @(rf/subscribe [:history/undos]))]

   [comp/icon-button {:title "Undo"
                      :icon "redo"
                      :action #(rf/dispatch [:history/redo 1])
                      :disabled? (not @(rf/subscribe [:history/redos?]))}]

   [:select.icon-button
    {:onChange #(rf/dispatch [:history/redo (-> % .-target .-value js/parseInt)])
     :disabled (not @(rf/subscribe [:history/redos?]))
     :style {:margin-left "-2px"
             :max-width "14px"
             :background "var(--level-0)"
             :font-size "1em"}}
    (history/select-options @(rf/subscribe [:history/redos]))]])

(defn close-button
  [key]
  [:button.icon-button.small.close-document-button
   {:key key
    :title "Close document"
    :on-pointer-down #(.stopPropagation %)
    :on-pointer-up #(do (.stopPropagation %)
                      (rf/dispatch [:document/close key]))} [comp/icon "times"]])

(defn context-menu
  [key]
  [{:text "Close"
    :action [:document/close key]}
   {:text "Close Others"
    :action [:document/close-others]}
   {:text "Close Saved"
    :action [:document/close-saved]}
   {:text "Close All"
    :action [:document/close-all]}
   {:key :divider-1
    :type :separator}
   {:text "Copy Path"}
   {:text "Open Containing Folder"}])

(defn tab
  []
  (let [dragged-over? (ra/atom false)]
    (fn [key document active?]
      [:> ContextMenu/Root
       [:> ContextMenu/Trigger
        [:div.flex.button.document-tab
         {:class (when active? "active")
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
                                   key]))
          :style {:background-color (if (or active? @dragged-over?)
                                      "var(--level-2)"
                                      "var(--level-1)")}}
         [:span.document-name (:title document)]
         [close-button key]]]
       [:> ContextMenu/Portal
        (into [:> ContextMenu/Content {:class "menu-content context-menu-content"}]
              (map (fn [item] [comp/context-menu-item item]) (context-menu key)))]])))

(defn tab-bar []
  (let [documents @(rf/subscribe [:documents])
        document-tabs @(rf/subscribe [:document-tabs])
        active-document @(rf/subscribe [:active-document])]
    [:div.flex.drag.justify-between {:style {:flex "0 0 40px"}}
     [:div.flex {:style {:flex "1 0 auto"}}
      (map (fn [document]
             ^{:key document}
             [tab document (document documents) (= document active-document)])
           document-tabs)]
     [:div.toolbar
      [:button.p-1.icon-button {:title "Document Actions"}
       [comp/icon "ellipsis-h"]]]]))
