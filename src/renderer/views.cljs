(ns renderer.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["react-resizable-panels" :refer [Panel PanelGroup PanelResizeHandle]]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.attribute.views :as attr]
   [renderer.cmdk.views :as cmdk]
   [renderer.codemirror.views :as cm]
   [renderer.components :as comp]
   [renderer.debug :as debug]
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

(defn command-input []
  [:div.flex.flex-col.level-0.relative.overflow-visible
   {:style {:font-size "10px"
            :user-select "text"}}
   [repl/main-view]])

(defn frame-panel
  []
  (let [rulers? @(rf/subscribe [:rulers?])]
    [:div.flex.flex-col.flex-1
     [:div.mb-px [toolbar.tools/root]
      (when rulers?
        [:div.flex
         [:div.level-2 {:style {:width "23px" :height "23px"}}
          [comp/toggle-icon-button
           {:active? @(rf/subscribe [:rulers-locked?])
            :active-icon "lock"
            :active-text "unlock"
            :inactive-icon "unlock"
            :inactive-text "lock"
            :class "small"
            :action #(rf/dispatch [:toggle-rulers-locked])}]]
         [:div.w-full.ml-px.level-2
          [rulers/ruler {:orientation :horizontal :size 23}]]])]
     [:div.flex.flex-1.relative
      [:<> (when rulers?
             [:div.level-2.mr-px
              [rulers/ruler {:orientation :vertical :size 23}]])]
      [:div.relative.grow.flex
       [frame/main]
       [:div.absolute.bottom-1.left-2.pointer-events-none
        {:style {:color "#555"}}
        @(rf/subscribe [:message])]
       (when @(rf/subscribe [:debug-info?])
         [:<>
          [debug/info]
          [debug/fps]])
       (when @(rf/subscribe [:backdrop?])
         [:div.backdrop
          {:on-click #(rf/dispatch [:set-backdrop false])}])]]]))

(defn editor
  []
  [:> PanelGroup
   {:direction "vertical"
    :id "editor-group"
    :autoSaveId "editor-group"}
   [:> Panel {:id "editor-panel"
              :minSize 10
              :order 1}
    [:div.flex.flex-col.flex-1.h-full
     [:div.flex.flex-1.overflow-hidden
      [frame-panel]
      (when @(rf/subscribe [:panel/visible? :history])
        [:div.v-scroll.p-1.level-2
         {:style {:flex "0 1 30%"}}])]
     [toolbar.status/root]
     [history/tree]]]
   (when @(rf/subscribe [:panel/visible? :timeline])
     [:<>
      [:> PanelResizeHandle
       {:id "timeline-resize-handle"
        :className "resize-handle"}]
      [:> Panel
       {:id "timeline-panel"
        :minSize 10
        :defaultSize 20
        :order 2
        :collapsedSize 15}
       [timeline/root]]])])

(defn root
  []
  (let [tree? @(rf/subscribe [:panel/visible? :tree])
        right-sidebar-min-width @(rf/subscribe [:window/right-sidebar-min-width])
        left-sidebar-min-width @(rf/subscribe [:window/left-sidebar-min-width])]
    [:> Tooltip/Provider
     [:div.flex.flex-col.flex-1.h-screen
      [win/app-header]
      (if (seq @(rf/subscribe [:documents]))
        [:> PanelGroup
         {:direction "horizontal"
          :id "root-group"
          :autoSaveId "root-group"}
         (when left-sidebar-min-width
           [:> Panel
            {:id "tree-panel"
             :collapsible true
             :minSize left-sidebar-min-width
             :defaultSize left-sidebar-min-width
             :onCollapse #(rf/dispatch-sync [:panel/collapse :tree])
             :onExpand #(rf/dispatch-sync [:panel/expand :tree])}
            (when tree?
              [:<>
               [doc/actions]
               [tree/root]])])
         [:> PanelResizeHandle
          {:id "tree-resize-handle"
           :className "resize-handle"}]
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
              :order 1}
             [:div.flex.h-full.flex-col [:> PanelGroup
                                         {:direction "horizontal"
                                          :id "center-top-group"
                                          :autoSaveId "center-top-group"}
                                         [:> Panel
                                          {:id "center-top-panel"
                                           :order 1}
                                          [editor]]
                                         (when @(rf/subscribe [:panel/visible? :xml])
                                           (let [xml @(rf/subscribe [:element/xml])]
                                             [:<>
                                              [:> PanelResizeHandle
                                               {:id "xml-resize-handle"
                                                :className "resize-handle"}]
                                              [:> Panel {:id "xml-panel"
                                                         :defaulSize 30
                                                         :minSize 10
                                                         :order 2}
                                               [:div.v-scroll.p-1.h-full.level-2.ml-px
                                                [cm/editor xml
                                                 {:options {:mode "text/xml"
                                                            :readOnly true}}]]]]))]
              [command-input]]]

            [:> PanelResizeHandle
             {:id "properties-resize-handle"
              :className "resize-handle"}]
            (when right-sidebar-min-width
              [:> Panel
               {:id "properties-panel"
                :collapsible true
                :order 2
                :minSize right-sidebar-min-width
                :defaultSize right-sidebar-min-width
                :onCollapse #(rf/dispatch-sync [:panel/collapse :properties])
                :onExpand #(rf/dispatch-sync [:panel/expand :properties])}
               (when @(rf/subscribe [:panel/visible? :properties])
                 [attr/form])])
            [toolbar.object/root]]]]]
        [home/panel])]

     [cmdk/dialog]

     [notification/main]]))
