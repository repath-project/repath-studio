(ns renderer.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["path-browserify" :as path]
   ["react-resizable-panels" :refer [Panel PanelGroup PanelResizeHandle]]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.attribute.views :as attr.v]
   [renderer.codemirror.views :as cm.v]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.views :as dialog.v]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.document.views :as document.v]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.views :as frame.v]
   [renderer.history.views :as history.v]
   [renderer.notification.views :as notification]
   [renderer.reepl.views :as repl.v]
   [renderer.ruler.views :as ruler.v]
   [renderer.tool.overlay :as overlay]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree.v]
   [renderer.ui :as ui]
   [renderer.window.events :as-alias window.e]
   [renderer.window.views :as window.v]))

(defn frame-panel
  []
  (let [rulers? @(rf/subscribe [:rulers-visible?])
        read-only? @(rf/subscribe [::document.s/read-only?])]
    [:div.flex.flex-col.flex-1.h-full
     [:div.mb-px
      [ui/scroll-area [toolbar.tools/root]]
      (when rulers?
        [:div.flex
         [:div.bg-primary {:style {:width "23px" :height "23px"}}
          #_[ui/toggle-icon-button
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
       [frame.v/root]
       (if read-only?
         [:div.absolute.inset-0.border-4.border-accent]
         (when @(rf/subscribe [:debug-info?])
           [:<>
            [overlay/debug-info]
            [ui/fps]]))
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
         [:div.bg-primary.h-full.ml-px
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

            [:div.h-full.bg-primary.ml-px.flex
             [ui/scroll-area
              [:div.p-1
               [cm.v/editor xml
                {:options {:mode "text/xml"
                           :readOnly true}}]]]]]]))]]])

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

(def paper-size
  {0 [2384 3370]
   1 [1684 2384]
   2 [1191 1684]
   3 [842 1191]
   4 [595 842]
   5 [420 595]
   6 [298 420]
   7 [210 298]
   8 [147 210]
   9 [105 147]
   10 [74 105]})

(defn home []
  (let [recent @(rf/subscribe [::document.s/recent])]
    [:div.flex.flex-1.min-h-full.justify-center.p-2.overflow-hidden
     [:div.self-center.justify-between.overflow-hidden.flex.h-full.md:h-auto.w-full.lg:w-auto
      [ui/scroll-area
       [:div.bg-primary.p-6.lg:p-12.flex.max-w-screen-xl.w-full.gap-8
        [:div.flex-1
         [:h1.text-4xl.mb-1.font-light config/app-name]

         [:p.text-xl.text-muted.font-bold
          "Scalable Vector Graphics Manipulation"]

         [:h2.mb-3.mt-8.text-2xl "Start"]

         [:div.flex.items-center.gap-2.flex-wrap
          [ui/icon "file"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::document.e/new])} "New"]
          [ui/shortcuts [::document.e/new]]

          [:span "or"]

          [:> Select/Root
           {:onValueChange #(rf/dispatch [::document.e/new-from-template (get paper-size %)])}
           [:> Select/Trigger
            {:class "select-trigger"
             :aria-label "Select size"}
            [:div.flex.items-center
             [:> Select/Value {:placeholder "Select template"}]
             [:> Select/Icon
              [ui/icon "chevron-down" {:class "icon small ml-2"}]]]]
           [:> Select/Portal
            [:> Select/Content
             {:class "menu-content rounded select-content"
              :style {:min-width "auto"}}

             [:> Select/Viewport {:class "select-viewport"}
              [:> Select/Group
               [:> Select/Item
                {:value :empty-canvas
                 :class "menu-item select-item"}
                [:> Select/ItemText "Empty canvas"]]
               (for [[k _v] (sort paper-size)]
                 ^{:key k}
                 [:> Select/Item
                  {:value k
                   :class "menu-item select-item"}
                  [:> Select/ItemText (str "A" k)]])]]]]]]

         [:div.flex.items-center.gap-2
          [ui/icon "folder"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::document.e/open])}
           "Open"]
          [ui/shortcuts [::document.e/open]]]

         (when (seq recent)
           [:<> [:h2.mb-3.mt-8.text-2xl "Recent"]

            (for [file-path (take 2 recent)]
              ^{:key file-path}
              [:div.flex.items-center.gap-x-2.flex-wrap
               [ui/icon "folder"]
               [:button.button-link.text-lg
                {:on-click #(rf/dispatch [::document.e/open file-path])}
                (.basename path file-path)]
               [:span.text-lg.text-muted (.dirname path file-path)]])])

         [:h2.mb-3.mt-8.text-2xl "Help"]

         [:div
          [:div.flex.items-center.gap-2
           [ui/icon "command"]
           [:button.button-link.text-lg
            {:on-click #(rf/dispatch [::dialog.e/cmdk])}
            "Command panel"]
           [ui/shortcuts [::dialog.e/cmdk]]]]
         [:div.flex.items-center.gap-2
          [ui/icon "earth"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::window.e/open-remote-url
                                     "https://repath.studio/"])}
           "Website"]]
         [:div.flex.items-center.gap-2
          [ui/icon "commit"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::window.e/open-remote-url
                                     "https://github.com/repath-project/repath-studio"])}
           "Source Code"]]
         [:div.flex.items-center.gap-2
          [ui/icon "list"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::window.e/open-remote-url
                                     "https://repath.studio/roadmap/changelog/"])}
           "Changelog"]]]

        [:div.hidden.md:block.flex-1
         [:img {:src "img/icon.svg"}]]]]]]))

(defn root
  []
  [:> Tooltip/Provider
   [:div.flex.flex-col.flex-1.h-screen.overflow-hidden
    [window.v/app-header]
    (if (seq @(rf/subscribe [:documents]))
      [:div.flex.h-full.flex-1.overflow-hidden
       (when @(rf/subscribe [:panel-visible? :tree])
         [:div.flex-col.hidden.md:flex.overflow-hidden
          {:style {:width "227px"}}
          [document.v/actions]
          [tree.v/root]])
       [:div.flex.flex-col.flex-1.overflow-hidden.h-full
        [document.v/tab-bar]
        [:div.flex.h-full.flex-1.overflow-hidden
         [:div.flex.h-full.flex-col.flex-1.overflow-hidden
          [editor]]
         (when @(rf/subscribe [:panel-visible? :properties])
           [:div.hidden.md:flex
            {:style {:flex "0 0 300px"}}
            [attr.v/form]])
         [:div.bg-primary.ml-px.flex
          [ui/scroll-area [toolbar.object/root]]]]]]
      [home])]
   [dialog.v/root]
   [notification/main]])
