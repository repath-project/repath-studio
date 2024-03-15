(ns renderer.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["react-resizable-panels" :refer [Panel PanelGroup PanelResizeHandle]]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.attribute.views :as attr]
   [renderer.codemirror.views :as cm]
   [renderer.components :as comp]
   [renderer.debug :as debug]
   [renderer.dialog.views :as dialog]
   [renderer.document.views :as doc]
   [renderer.frame.views :as frame]
   [renderer.history.views :as history]
   [renderer.home :as home]
   [renderer.notification.views :as notification]
   [renderer.reepl.views :as repl]
   [renderer.rulers.views :as rulers]
   [renderer.timeline.views :as timeline]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree]
   [renderer.window.views :as win]))

(defn frame-panel
  []
  (let [rulers? @(rf/subscribe [:rulers?])
        read-only? @(rf/subscribe [:document/read-only?])]
    [:div.flex.flex-col.flex-1.h-full
     [:div.mb-px [toolbar.tools/root]
      (when rulers?
        [:div.flex
         [:div.bg-primary {:style {:width "23px" :height "23px"}}
          [comp/toggle-icon-button
           {:active? @(rf/subscribe [:rulers-locked?])
            :active-icon "lock"
            :active-text "unlock"
            :inactive-icon "unlock"
            :inactive-text "lock"
            :class "small"
            :action #(rf/dispatch [:toggle-rulers-locked])}]]
         [:div.w-full.ml-px.bg-primary
          [rulers/ruler {:orientation :horizontal :size 23}]]])]
     [:div.flex.flex-1.relative
      [:<>
       (when rulers?
         [:div.bg-primary.mr-px
          [rulers/ruler {:orientation :vertical :size 22}]])]
      [:div.relative.grow.flex
       [frame/main]
       (if read-only?
         [:div.absolute.inset-0.border-4.border-accent]
         [:<>
          [:div.absolute.bottom-1.left-2.pointer-events-none
           {:style {:color "#555"}}
           @(rf/subscribe [:message])]
          (when @(rf/subscribe [:debug-info?])
            [:<>
             [debug/info]
             [debug/fps]])])
       (when @(rf/subscribe [:backdrop?])
         [:div.absolute.inset-0
          {:on-click #(rf/dispatch [:set-backdrop false])}])]]]))

(defn center-top-group
  []
  [:div.flex.flex-col.flex-1.h-full
   [:> PanelGroup
    {:direction "horizontal"
     :id "center-top-group"
     :autoSaveId "center-top-group"}
    [:div.flex.flex-1.overflow-hidden
     [:> Panel
      {:id "frame-panel"
       :order 1}
      [frame-panel]]
     (when @(rf/subscribe [:panel/visible? :history])
       [:<>
        [:> PanelResizeHandle
         {:id "history-resize-handle"
          :className "resize-handle"}]
        [:> Panel {:id "history-panel"
                   :defaultSize 30
                   :minSize 5
                   :order 2}
         [:div.v-scroll.p-1.bg-primary.h-full.ml-px
          [history/root]]]])

     (when @(rf/subscribe [:panel/visible? :xml])
       (let [xml @(rf/subscribe [:element/xml])]
         [:<>
          [:> PanelResizeHandle
           {:id "xml-resize-handle"
            :className "resize-handle"}]
          [:> Panel {:id "xml-panel"
                     :defaultSize 30
                     :minSize 5
                     :order 3}
           [:div.v-scroll.p-1.h-full.bg-primary.ml-px
            [cm/editor xml
             {:options {:mode "text/xml"
                        :readOnly true}}]]]]))]]
   [toolbar.status/root]])

(defn editor
  []
  (let [timeline? @(rf/subscribe [:panel/visible? :timeline])
        repl-history? @(rf/subscribe [:panel/visible? :repl-history])]
    [:> PanelGroup
     ;; REVIEW: We need to rerender the group to properly resize the panels.
     {:key (str timeline? repl-history?)
      :direction "vertical"
      :id "editor-group"
      :autoSaveId "editor-group"}
     [:> Panel {:id "editor-panel"
                :minSize 20
                :order 1}
      [center-top-group]]
     [timeline/root]
     [repl/root]]))

(defn tree-panel
  []
  (when @(rf/subscribe [:panel/visible? :tree])
    [:<>
     [:> Panel
      {:id "tree-panel"
       :class "flex flex-col"
       :defaultSize 5
       :style {:min-width "227px"}}
      [doc/actions]
      [tree/root]]
     [:> PanelResizeHandle
      {:id "tree-resize-handle"
       :className "resize-handle"}]]))

(defn root
  []
  [:> Tooltip/Provider
   [:div.flex.flex-col.flex-1.h-screen
    [win/app-header]
    (if (seq @(rf/subscribe [:documents]))
      [:> PanelGroup
       {:direction "horizontal"
        :id "root-group"
        :autoSaveId "root-group"}
       [tree-panel]
       [:> Panel
        {:id "main-panel"
         :order 2}
        [:div.flex.flex-col.flex-1.overflow-hidden.h-full
         [doc/tab-bar]
         [:> PanelGroup
          {:direction "horizontal"
           :id "center-group"
           :autoSaveId "center-group"}
          [:> Panel
           {:id "center-panel"
            :minSize 10
            :order 1}
           [:div.flex.h-full.flex-col
            [editor]]]
          (when @(rf/subscribe [:panel/visible? :properties])
            [:<>
             [:> PanelResizeHandle
              {:id "properties-resize-handle"
               :className "resize-handle"}]
             [:> Panel
              {:id "properties-panel"
               :order 2
               :defaultSize 5
               :style {:min-width "300px"}}
              [attr/form]]])
          [toolbar.object/root]]]]]
      [home/panel])]
   [dialog/root]
   [notification/main]])
