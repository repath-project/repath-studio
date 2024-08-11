(ns renderer.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["react-resizable-panels" :refer [Panel PanelGroup PanelResizeHandle]]
   [re-frame.core :as rf]
   [renderer.attribute.views :as attr.v]
   [renderer.codemirror.views :as cm.v]
   [renderer.components :as comp]
   [renderer.debug :as debug]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.views :as dialog.v]
   [renderer.document.subs :as-alias document.s]
   [renderer.document.events :as-alias document.e]
   [renderer.document.views :as document.v]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.views :as frame.v]
   [renderer.history.views :as history.v]
   [renderer.notification.views :as notification]
   [renderer.reepl.views :as repl.v]
   [renderer.ruler.views :as ruler.v]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree.v]
   [renderer.window.views :as window.v]
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
       [frame.v/main]
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
          [history.v/root]]]])

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
            [cm.v/editor xml
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
     [repl.v/root]]))

(defn home []
  (let [recent @(rf/subscribe [::document.s/recent])]
    [:div.flex.overflow-auto.flex-1.min-h-full.justify-center
     [:div.bg-primary.w-full.self-center.justify-between.p-12.flex.max-w-screen-xl
      [:div
       [:h1.text-4xl.mb-1.font-light
        "Repath Studio"]

       [:p.text-xl.text-muted.font-bold
        "Scalable Vector Graphics Manipulation"]

       [:h2.mb-3.mt-8.text-2xl "Start"]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [::document.e/new])} "New"]
        [comp/shortcuts [::document.e/new]]]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [::document.e/open])}
         "Open"]
        [comp/shortcuts [::document.e/open]]]

       [:h2.mb-3.mt-8.text-2xl
        {:class (when-not (seq recent) "text-muted")}
        "Recent"]

       (for [file-path (take 2 recent)]
         ^{:key file-path}
         [:button.text-lg.text-accent.block
          {:on-click #(rf/dispatch [::document.e/open file-path])}
          file-path])

       [:h2.mb-3.mt-8.text-2xl "Help"]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [::dialog.e/cmdk])}
         "Command panel"]
        [comp/shortcuts [::dialog.e/cmdk]]]
       [:a.text-lg.block
        {:href "https://repath.studio/"
         :target "_blank"}
        "Website"]
       [:a.text-lg.block
        {:href "https://github.com/repath-project/repath-studio"
         :target "_blank"}
        "Source Code"]
       [:a.text-lg.block
        {:href "https://repath.studio/roadmap/changelog/"
         :target "_blank"}
        "Changelog"]]

      [:div
       [:img {:src "img/icon.svg"}]]]]))

(defn root
  []
  [:> Tooltip/Provider
   [:div.flex.flex-col.flex-1.h-screen
    [window.v/app-header]
    (if (seq @(rf/subscribe [:documents]))
      [:div.flex.h-full.flex-1.overflow-hidden
       (when @(rf/subscribe [:panel-visible? :tree])
         [:div.flex.flex-col
          {:style {:width "227px"}}
          [document.v/actions]
          [tree.v/root]])
       [:div.flex.flex-col.flex-1.overflow-hidden.h-full
        [document.v/tab-bar]
        [:div.flex.h-full.flex-1
         [:div.flex.h-full.flex-col.flex-1.overflow-hidden
          [editor]]
         (when @(rf/subscribe [:panel-visible? :properties])
           [:div.flex
            {:style {:flex "0 0 300px"}}
            [attr.v/form]])
         [toolbar.object/root]]]]
      [home])]
   [dialog.v/root]
   [notification/main]])
