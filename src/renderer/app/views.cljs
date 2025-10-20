(ns renderer.app.views
  (:require
   ["@radix-ui/react-direction" :as Direction]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["path-browserify" :as path-browserify]
   ["react-fps" :refer [FpsView]]
   ["react-resizable-panels" :refer [Panel PanelGroup]]
   ["sonner" :refer [Toaster]]
   ["vaul" :refer [Drawer]]
   [clojure.string :as string]
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
   [renderer.frame.subs :as-alias frame.subs]
   [renderer.frame.views :as frame.views]
   [renderer.history.views :as history.views]
   [renderer.reepl.views :as repl.views]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.ruler.views :as ruler.views]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.theme.subs :as-alias theme.subs]
   [renderer.timeline.views :as timeline.views]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]
   [renderer.window.views :as window.views]
   [renderer.worker.subs :as-alias worker.subs]))

(defn coll->str
  [coll]
  (str "[" (string/join " " (map utils.length/->fixed coll)) "]"))

(defn map->str
  [m]
  (->> m
       (map (fn [[k v]]
              ^{:key k}
              [:span (str (name k) ": " (if (number? v)
                                          (utils.length/->fixed v)
                                          (coll->str v)))]))
       (interpose ", ")))

(defn debug-rows
  []
  (let [viewbox (rf/subscribe [::frame.subs/viewbox])
        pointer-pos (rf/subscribe [::app.subs/pointer-pos])
        adjusted-pos (rf/subscribe [::app.subs/adjusted-pointer-pos])
        pointer-offset (rf/subscribe [::app.subs/pointer-offset])
        adjusted-offset (rf/subscribe [::app.subs/adjusted-pointer-offset])
        drag? (rf/subscribe [::tool.subs/drag?])
        pan (rf/subscribe [::document.subs/pan])
        active-tool (rf/subscribe [::tool.subs/active])
        cached-tool (rf/subscribe [::tool.subs/cached])
        tool-state (rf/subscribe [::tool.subs/state])
        clicked-element (rf/subscribe [::app.subs/clicked-element])
        ignored-ids (rf/subscribe [::document.subs/ignored-ids])
        nearest-neighbor (rf/subscribe [::snap.subs/nearest-neighbor])]
    [["Viewbox" (coll->str @viewbox)]
     ["Pointer position" (coll->str @pointer-pos)]
     ["Adjusted pointer position" (coll->str @adjusted-pos)]
     ["Pointer offset" (coll->str @pointer-offset)]
     ["Adjusted pointer offset" (coll->str @adjusted-offset)]
     ["Pointer drag?" (str @drag?)]
     ["Pan" (coll->str @pan)]
     ["Active tool" @active-tool]
     ["Cached tool" @cached-tool]
     ["State" @tool-state]
     ["Clicked element" (:id @clicked-element)]
     ["Ignored elements" @ignored-ids]
     ["Snap" (map->str @nearest-neighbor)]]))

(defn debug-info
  []
  [:div
   {:dir "ltr"}; }
   (into [:div.absolute.bottom-2.left-2.bg-primary.p-2.shadow]
         (for [[s v] (debug-rows)]
           [:div.flex
            [:strong.mr-1 s]
            [:div v]]))
   [:div.fps-wrapper
    [:> FpsView #js {:width 240
                     :height 180}]]])

(defn help
  [message]
  [:div.hidden.justify-center.w-full.p-4.sm:flex
   [:div.bg-primary.overflow-hidden.shadow.rounded-full
    [:div.text-xs.gap-1.flex.flex-wrap.py-2.px-4.justify-center.truncate
     {:aria-live "polite"}
     message]]])

(defn read-only-overlay []
  [:div.absolute.inset-0.border-4.border-accent
   (when-let [preview-label @(rf/subscribe [::document.subs/preview-label])]
     [:div.absolute.bg-accent.top-2.left-2.px-1.rounded.text-accent-foreground
      preview-label])])

(defn right-panel
  [active-tool]
  [:div.flex.flex-col.h-full.w-80.bg-secondary
   [views/scroll-area
    (tool.hierarchy/right-panel active-tool)]
   [:div.bg-primary.grow.flex]])

(defn frame-panel
  []
  (let [ruler-visible? @(rf/subscribe [::ruler.subs/visible?])
        ruler-locked? @(rf/subscribe [::ruler.subs/locked?])
        backdrop @(rf/subscribe [::app.subs/backdrop])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])
        some-selected? @(rf/subscribe [::element.subs/some-selected?])
        active-tool @(rf/subscribe [::tool.subs/active])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        help-message @(rf/subscribe [::tool.subs/help])
        help-bar @(rf/subscribe [::app.subs/help-bar])
        debug-info? @(rf/subscribe [::app.subs/debug-info])
        worker-active? @(rf/subscribe [::worker.subs/some-active?])]
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
       [:div.grow.flex
        {:data-theme "light"
         :style {:background "var(--secondary)"}}
        [frame.views/root]]
       [:div.absolute.inset-0.pointer-events-none.inset-shadow
        (when read-only?
          [read-only-overlay])
        (when debug-info?
          [debug-info])
        (when worker-active?
          [:button.icon-button.absolute.bottom-2.right-2
           [views/loading-indicator]])
        (when (and help-bar (seq help-message))
          [help help-message])]
       (when backdrop
         [:div.absolute.inset-0
          {:on-click #(rf/dispatch [::app.events/set-backdrop false])}])

       (when (not md?)
         [:> Drawer.Root {:direction "left"}
          [:> Drawer.Trigger
           {:class "button px-2 absolute bottom-2 left-2 rounded bg-primary!
                    shadow-md"
            :aria-label (t [::toggle-properties "Toggle tree panel"])}
           [views/icon "tree"]]
          [:> Drawer.Portal
           [:> Drawer.Overlay {:class "backdrop"}]
           [:> Drawer.Content
            {:class "inset-0 right-auto fixed z-10 outline-none flex"}
            [:> Drawer.Title {:class "sr-only"}
             (t [::properties-panel "Tree panel"])]
            [:> Drawer.Description
             {:as-child true}
             [:div.flex.overflow-hidden.shadow-xl
              [tree.views/root]]]]]])
       (when (and (not md?)
                  some-selected?)
         [:> Drawer.Root {:direction "right"}
          [:> Drawer.Trigger
           {:class "button px-2 absolute bottom-2 right-2 rounded bg-primary!
                    shadow-md"
            :aria-label (t [::toggle-properties "Toggle attributes panel"])}
           [views/icon "properties"]]
          [:> Drawer.Portal
           [:> Drawer.Overlay {:class "backdrop"}]
           [:> Drawer.Content
            {:class "inset-0 left-auto fixed z-10 overflow-hidden
                     flex"}
            [:> Drawer.Title {:class "sr-only"}
             (t [::properties-panel "Attributes panel"])]
            [:> Drawer.Description
             {:as-child true}
             [:div.flex.overflow-hidden.shadow-xl
              [right-panel active-tool]]]]]])]]]))

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
       (let [xml @(rf/subscribe [::element.subs/xml])
             codemirror-theme @(rf/subscribe [::theme.subs/codemirror])]
         [:<>
          [views/resize-handle "xml-resize-handle"]
          [:> Panel {:id "xml-panel"
                     :defaultSize 30
                     :minSize 5
                     :order 3}

           [:div.h-full.bg-primary.flex
            [views/scroll-area
             [:div.p-1
              [views/cm-editor xml
               {:options {:mode "text/xml"
                          :readOnly true
                          :screenReaderLabel "XML"
                          :theme codemirror-theme}}]]]]]]))]]])

(defn editor
  []
  (let [timeline-visible @(rf/subscribe [::app.subs/panel-visible? :timeline])]
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
     [repl.views/root]]))

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
               {:class "md:flex w-[227px]"}
               [document.views/actions]
               [tree.views/root]])
            [:div.flex.flex-col.flex-1.overflow-hidden.h-full
             [document.views/tab-bar]
             [:div.flex.h-full.flex-1.gap-px.overflow-hidden
              [:div.flex.h-full.flex-col.flex-1.overflow-hidden.gap-px
               [editor]]
              [:div.flex.gap-px
               (when (and md? properties?)
                 [right-panel active-tool])
               [:div.bg-primary.flex
                [views/scroll-area [toolbar.object/root]]]]]]]
           [home recent-documents])
         [:div]]
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
