(ns renderer.app.views
  (:require
   ["@radix-ui/react-direction" :as Direction]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["path-browserify" :as path-browserify]
   ["react-resizable-panels" :refer [Panel PanelGroup]]
   ["sonner" :refer [Toaster]]
   [config :as config]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.db :as db]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.dialog.views :as dialog.views]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.document.views :as document.views]
   [renderer.element.subs :as-alias element.subs]
   [renderer.events :as-alias events]
   [renderer.frame.views :as frame.views]
   [renderer.history.views :as history.views]
   [renderer.reepl.views :as repl.views]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.ruler.views :as ruler.views]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.timeline.views :as timeline.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]
   [renderer.window.views :as window.views]))

(defn right-panel
  [active-tool]
  [:div.flex.flex-col.h-full.bg-secondary
   [views/scroll-area
    (tool.hierarchy/right-panel active-tool)]
   [:div.bg-primary.grow.flex]])

(defn frame-panel
  []
  (let [ruler-visible? @(rf/subscribe [::ruler.subs/visible?])
        ruler-locked? @(rf/subscribe [::ruler.subs/locked?])
        backdrop @(rf/subscribe [::app.subs/backdrop])]
    [:div.flex.flex-col.flex-1.h-full.gap-px
     [:div
      [views/scroll-area [toolbar.tools/root]]
      (when ruler-visible?
        [:div.flex.gap-px
         [:div.bg-primary
          {:style {:width ruler.views/ruler-size
                   :height ruler.views/ruler-size}}
          [views/icon-button
           (if ruler-locked? "lock" "unlock")
           {:class "small bg-transparent! hidden"
            :title (if ruler-locked?
                     (t [::unlock "Unlock"])
                     (t [::lock "Lock"]))
            :on-click #(rf/dispatch [::ruler.events/toggle-locked])}]]
         [:div.bg-primary.flex-1
          {:dir "ltr"}
          [ruler.views/ruler :horizontal]]])]
     [:div.flex.flex-1.relative.gap-px
      (when ruler-visible?
        [:div.bg-primary
         {:dir "ltr"
          :class "rtl:scale-x-[-1]"}
         [ruler.views/ruler :vertical]])
      [:div.relative.grow.flex
       [:div.grow.flex.bg
        {:data-theme "light"
         :style {:background "var(--secondary)"}}
        [frame.views/root]]
       (when backdrop
         [:div.absolute.inset-0
          {:on-click #(rf/dispatch [::app.events/set-backdrop false])}])]]]))

(defn xml-panel
  []
  (let [xml @(rf/subscribe [::element.subs/xml])
        codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
    [views/scroll-area
     [:div.p-1
      [views/cm-editor xml
       {:options {:mode "text/xml"
                  :readOnly true
                  :screenReaderLabel "XML"
                  :theme codemirror-theme}}]]]))
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
     (when @(rf/subscribe [::app.subs/panel-visible? :history])
       [:<>
        [views/resize-handle "history-resize-handle"]
        [:> Panel {:id "history-panel"
                   :defaultSize 30
                   :minSize 5
                   :order 2}
         [:div.bg-primary.h-full
          [history.views/root]]]])

     (when @(rf/subscribe [::app.subs/panel-visible? :xml])
       [:<>
        [views/resize-handle "xml-resize-handle"]
        [:> Panel {:id "xml-panel"
                   :defaultSize 30
                   :minSize 5
                   :order 3}

         [:div.h-full.bg-primary.flex
          [xml-panel]]]])]]])

(defn editor
  []
  (let [timeline-visible @(rf/subscribe [::app.subs/panel-visible? :timeline])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])]
    [:> PanelGroup
     {:direction "vertical"
      :id "editor-group"
      :autoSaveId "editor-group"}
     [:> Panel {:id "editor-panel"
                :minSize 20
                :order 1}
      [center-top-group]]
     [toolbar.status/root]
     (when timeline-visible
       [views/resize-handle "timeline-resize-handle"])
     (when timeline-visible
       [:> Panel
        {:id "timeline-panel"
         :minSize 10
         :defaultSize 20
         :order 2}
        [timeline.views/root]])
     (when md?
       [repl.views/root])]))

(defn document-size-select []
  [:> Select/Root
   {:onValueChange #(rf/dispatch [::document.events/new-from-template
                                  (get db/a-series-paper-sizes %)])}
   [:> Select/Trigger
    {:class "button px-2 bg-overlay rounded-sm"
     :aria-label (t [::select-size "Select size"])}
    [:div.flex.items-center.gap-2
     [:> Select/Value
      {:placeholder (t [::select-template "Select template"])}]
     [:> Select/Icon
      [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:class "menu-content rounded-sm select-content"
      :style {:min-width "auto"}
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/Viewport
      {:class "select-viewport"}
      [:> Select/Group
       [:> Select/Item
        {:value :empty-canvas
         :class "menu-item select-item"}
        [:> Select/ItemText
         (t [::empty-canvas "Empty canvas"])]]
       (for [[k _v] (sort db/a-series-paper-sizes)]
         ^{:key k}
         [:> Select/Item
          {:value k
           :class "menu-item select-item"}
          [:> Select/ItemText (str "A" k)]])]]]]])

(defn recent-document
  [{:keys [path title]
    :as recent}]
  [:div.flex.items-center.gap-x-2.flex-wrap
   [views/icon "folder"]
   [:button.button-link.text-lg
    {:on-click #(rf/dispatch [::document.events/open-recent recent])}
    (or title (.basename path-browserify path))]
   (when path
     [:span.text-lg.text-foreground-muted (.dirname path-browserify path)])])

(defn help-command
  [icon label event]
  [:div.flex.items-center.gap-2.flex-wrap
   [views/icon icon]
   [:button.button-link.text-lg
    {:on-click #(rf/dispatch event)}
    label]
   [views/shortcuts event]])

(defn home
  [recent-documents]
  [:div.flex.overflow-hidden
   [views/scroll-area
    [:div.flex.justify-center.p-2
     [:div.justify-around.flex
      [:div.flex.w-full
       [:div.flex-1
        [:div.p-4
         {:class "lg:p-12"}
         [:img.h-24.w-24.mb-3
          {:src "img/icon-no-bg.svg"
           :alt "logo"}]
         [:h1.text-4xl.mb-1.font-light config/app-name]

         [:p.text-xl.text-foreground-muted.font-bold
          (t [::svg-description "Scalable Vector Graphics Manipulation"])]

         [:h2.mb-3.mt-8.text-2xl (t [::start "Start"])]

         [:div.flex.items-center.gap-2.flex-wrap
          [views/icon "file"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::document.events/new])}
           (t [::new "New"])]
          [views/shortcuts [::document.events/new]]

          [:span (t [::or "or"])]

          [document-size-select]]

         [:div.flex.items-center.gap-2
          [views/icon "folder"]
          [:button.button-link.text-lg
           {:on-click #(rf/dispatch [::document.events/open])}
           (t [::open "Open"])]
          [views/shortcuts [::document.events/open]]]

         (when (seq recent-documents)
           [:<> [:h2.mb-3.mt-8.text-2xl
                 (t [::recent "Recent"])]

            (for [recent (take 5 recent-documents)]
              ^{:key (:id recent)}
              [recent-document recent])])

         [:h2.mb-3.mt-8.text-2xl
          (t [::help "Help"])]

         (->> [["command"
                (t [::command-panel "Command panel"])
                [::dialog.events/show-cmdk]]
               ["earth"
                (t [::website "Website"])
                [::events/open-remote-url
                 "https://repath.studio/"]]
               ["commit"
                (t [::source-code "Source Code"])
                [::events/open-remote-url
                 "https://github.com/repath-project/repath-studio"]]
               ["list"
                (t [::changelog "Changelog"])
                [::events/open-remote-url
                 "https://repath.studio/roadmap/changelog/"]]]
              (map #(apply help-command %))
              (into [:div]))]]]]]]])

(defn bottom-bar
  []
  (let [some-selected? @(rf/subscribe [::element.subs/some-selected?])
        active-tool @(rf/subscribe [::tool.subs/active])]
    [:div.flex.justify-evenly.p-2.gap-1

     [views/drawer
      {:icon "tree"
       :label "Tree"
       :direction "left"
       :content [tree.views/root]}]

     [views/drawer
      {:icon "code"
       :label "XML"
       :direction "left"
       :content [xml-panel]}]
     [:span.v-divider]

     [views/drawer
      {:icon "animation"
       :label "Timeline"
       :direction "bottom"
       :content [timeline.views/root]}]

     [views/drawer
      {:icon "shell"
       :label "Shell"
       :direction "bottom"
       :content [repl.views/root]}]

     [:span.v-divider]

     [views/drawer
      {:icon "history"
       :label "History"
       :direction "right"
       :content [history.views/root]}]

     [views/drawer
      {:icon "properties"
       :label "Attributes"
       :direction "right"
       :disabled (not some-selected?)
       :content [right-panel active-tool]}]]))

(defn root
  []
  (let [documents? @(rf/subscribe [::document.subs/entities?])
        tree? @(rf/subscribe [::app.subs/panel-visible? :tree])
        properties? @(rf/subscribe [::app.subs/panel-visible? :properties])
        active-tool @(rf/subscribe [::tool.subs/active])
        recent-documents @(rf/subscribe [::document.subs/recent])
        lang-dir @(rf/subscribe [::app.subs/lang-dir])
        web? @(rf/subscribe [::app.subs/web?])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])
        loading? @(rf/subscribe [::app.subs/loading?])
        theme @(rf/subscribe [::theme.subs/theme])]
    (if loading?
      (when web? [:div.loader])
      [:> Direction/Provider {:dir lang-dir}
       [:> Tooltip/Provider
        [:div.flex.flex-col.flex-1.h-dvh.overflow-hidden.justify-between
         [window.views/app-header]
         (if documents?
           [:div.flex.h-full.flex-1.overflow-hidden.gap-px
            (when tree?
              [:div.flex-col.hidden.overflow-hidden
               {:class "md:flex"}
               [document.views/actions]
               [tree.views/root]])
            [:div.flex.flex-col.flex-1.overflow-hidden.h-full
             [document.views/tab-bar]
             [:div.flex.h-full.flex-1.gap-px.overflow-hidden
              [:div.flex.h-full.flex-col.flex-1.overflow-hidden.gap-px
               [editor]]
              [:div.flex.gap-px
               (when (and md? properties?)
                 [:div.w-80 [right-panel active-tool]])
               [:div.bg-primary.flex
                [views/scroll-area [toolbar.object/root]]]]]]]
           [home recent-documents])
         (when (and documents? (not md?))
           [bottom-bar])]
        [dialog.views/root]
        [:> Toaster
         {:theme theme
          :toastOptions
          {:classNames {:toast "bg-primary! border! border-border! shadow-md!
                                p-4! rounded-md!"
                        :title "text-foreground-hovered!"
                        :description "text-foreground! text-xs"}}
          :icons {:success
                  (reagent/as-element
                   [views/icon "success" {:class "text-success"}])
                  :error
                  (reagent/as-element
                   [views/icon "error" {:class "text-error"}])
                  :warning
                  (reagent/as-element
                   [views/icon "warning" {:class "text-warning"}])
                  :info
                  (reagent/as-element [views/icon "info"])}}]]])))
