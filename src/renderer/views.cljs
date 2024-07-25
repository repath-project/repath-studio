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
   [renderer.document.subs :as-alias document.s]
   [renderer.document.views :as doc]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.views :as frame]
   [renderer.history.views :as history]
   [renderer.home :as home]
   [renderer.notification.views :as notification]
   [renderer.reepl.views :as repl]
   [renderer.ruler.views :as ruler.v]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree]
   [renderer.window.views :as win]
   [renderer.worker.subs :as-alias worker.s]))

(defn frame-panel
  []
  (let [rulers? @(rf/subscribe [:rulers-visible?])
        read-only? @(rf/subscribe [::document.s/read-only?])
        loading? @(rf/subscribe [::worker.s/loading?])]
    [:div.flex.flex-col.flex-1.h-full
     [:div.mb-px [toolbar.tools/root]
      (when rulers?
        [:div.flex
         [:div.bg-primary {:style {:width "23px" :height "23px"}}
          #_[comp/toggle-icon-button
             {:active? @(rf/subscribe [:rulers-locked?])
              :active-icon "lock"
              :active-text "unlock"
              :inactive-icon "unlock"
              :inactive-text "lock"
              :class "small"
              :action #(rf/dispatch [:toggle-rulers-locked])}]]
         [:div.w-full.ml-px.bg-primary
          [ruler.v/ruler {:orientation :horizontal :size 23}]]])]
     [:div.flex.flex-1.relative
      [:<>
       (when rulers?
         [:div.bg-primary.mr-px
          [ruler.v/ruler {:orientation :vertical :size 22}]])]
      [:div.relative.grow.flex
       [frame/main]
       (if read-only?
         [:div.absolute.inset-0.border-4.border-accent]
         (when @(rf/subscribe [:debug-info?])
           [:<>
            [debug/info]
            [debug/fps]]))
       [:div.absolute.bottom-0.left-0.flex.pointer-events-none.w-full.p-2
        {:style {:color "#555"}}
        [:div.grow.text-xs.truncate.flex.items-end
         @(rf/subscribe [:message])]
        (when loading?
          [:span.icon-button.relative
           {:style {:fill "#555"}}
           [comp/icon "spinner" {:class "loading"}]])]
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
     (when @(rf/subscribe [:panel-visible? :history])
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

     (when @(rf/subscribe [:panel-visible? :xml])
       (let [xml @(rf/subscribe [::element.s/xml])]
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
                        :readOnly true}}]]]]))]]])

(defn editor
  []
  (let [timeline? @(rf/subscribe [:panel-visible? :timeline])
        repl-history? @(rf/subscribe [:panel-visible? :repl-history])]
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
     [toolbar.status/root]
     [repl/root]]))

(defn root
  []
  [:> Tooltip/Provider
   [:div.flex.flex-col.flex-1.h-screen
    [win/app-header]
    (if (seq @(rf/subscribe [:documents]))
      [:div.flex.h-full.flex-1.overflow-hidden
       (when @(rf/subscribe [:panel-visible? :tree])
         [:div.flex.flex-col
          {:style {:width "227px"}}
          [doc/actions]
          [tree/root]])
       [:div.flex.flex-col.flex-1.overflow-hidden.h-full
        [doc/tab-bar]
        [:div.flex.h-full.flex-1
         [:div.flex.h-full.flex-col.flex-1.overflow-hidden
          [editor]]
         (when @(rf/subscribe [:panel-visible? :properties])
           [:div.flex
            {:style {:flex "0 0 300px"}}
            [attr/form]])
         [toolbar.object/root]]]]
      [home/panel])]
   [dialog/root]
   [notification/main]])
