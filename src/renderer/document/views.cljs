(ns renderer.document.views
  (:require
   ["@radix-ui/react-context-menu" :as ContextMenu]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [platform]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
   [renderer.history.views :as history]))

(defn actions []
  (let [undos? @(rf/subscribe [:history/undos?])
        redos? @(rf/subscribe [:history/redos?])]
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

     [:button.icon-button.items-center.px-1.gap-1
      {:title "Undo"
       :style {:margin-right 0
               :width "auto"
               :display "flex"}
       :on-click #(rf/dispatch [:history/undo])
       :disabled (not undos?)}
      [renderer.components/icon "undo"]
      [history/select
       "Undo stack"
       @(rf/subscribe [:history/undos])
       (not undos?)]]

     [:button.icon-button.items-center.px-1.gap-1
      {:title "Redo"
       :style {:margin-right 0
               :width "auto"
               :display "flex"}
       :on-click #(rf/dispatch [:history/redo])
       :disabled (not redos?)}
      [renderer.components/icon "redo"]
      [history/select
       "Redo stack"
       @(rf/subscribe [:history/redos])
       (not redos?)]]]))

(defn close-button
  [key saved?]
  [:button.close-document-button.small.hover:bg-transparent
   {:key key
    :title "Close document"
    :on-pointer-down #(.stopPropagation %)
    :on-pointer-up (fn [e]
                     (.stopPropagation e)
                     (rf/dispatch [:document/close key true]))}
   [comp/icon "times"]
   (when-not saved?
     [comp/icon "dot" {:class "icon dot"}])])

(defn context-menu
  [key]
  (let [document @(rf/subscribe [:document/document key])
        path (:path document)
        document-tabs @(rf/subscribe [:document-tabs])]
    [{:label "Close"
      :action [:document/close key true]}
     {:label "Close others"
      :action [:document/close-others]
      :disabled? (empty? (rest document-tabs))}
     {:label "Close all"
      :action [:document/close-all]}
     {:label "Close saved"
      :action [:document/close-saved]}
     {:type :separator}
     {:label "Open containing directory"
      :action [:document/open-directory path]
      :disabled? (not (and path platform/electron?))}]))

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
                              4 (rf/dispatch [:document/close key true])
                              1 (rf/dispatch [:document/set-active key])
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
     [:div.flex.flex-1.overflow-hidden
      (for [document document-tabs]
        ^{:key document}
        [tab document (document documents) (= document active-document)])]
     [:div.toolbar
      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:as-child true}
        [:button.button.flex.items-center.justify-center.aria-expanded:overlay.px-2.font-mono.rounded
         [comp/icon "ellipsis-h"]]]
       [:> DropdownMenu/Portal
        [:> DropdownMenu/Content
         {:class "menu-content rounded"}
         (for [item [{:label "Close all"
                      :key :close-all
                      :action [:document/close-all]}
                     {:label "Close saved"
                      :key :close-saved
                      :action [:document/close-saved]}]]
           ^{:key (:key item)} [comp/dropdown-menu-item item])
         [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]]))
