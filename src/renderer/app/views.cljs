(ns renderer.app.views
  (:require
   ["@radix-ui/react-direction" :as Direction]
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["path-browserify" :as path]
   ["react-fps" :refer [FpsView]]
   ["react-resizable-panels" :refer [Panel PanelGroup PanelResizeHandle]]
   [clojure.string :as string]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.events :as app.events]
   [renderer.app.subs :as-alias app.subs]
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
   [renderer.notification.views :as notification.views]
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
   [renderer.window.views :as window.views]))

(defn coll->str
  [coll]
  (str "[" (string/join " " (map utils.length/->fixed coll)) "]"))

(defn map->str
  [m]
  (interpose ", " (map (fn [[k v]]
                         ^{:key k}
                         [:span (str (name k) ": " (if (number? v)
                                                     (utils.length/->fixed v)
                                                     (coll->str v)))]) m)))

(defn debug-rows
  []
  (let [viewbox (rf/subscribe [::frame.subs/viewbox])
        pointer-pos (rf/subscribe [::app.subs/pointer-pos])
        adjusted-pointer-pos (rf/subscribe [::app.subs/adjusted-pointer-pos])
        pointer-offset (rf/subscribe [::app.subs/pointer-offset])
        adjusted-pointer-offset (rf/subscribe [::app.subs/adjusted-pointer-offset])
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
     ["Adjusted pointer position" (coll->str @adjusted-pointer-pos)]
     ["Pointer offset" (coll->str @pointer-offset)]
     ["Adjusted pointer offset" (coll->str @adjusted-pointer-offset)]
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
   (into [:div.absolute.top-1.left-2.pointer-events-none.text-color]
         (for [[s v] (debug-rows)]
           [:div.flex
            [:strong.mr-1 s]
            [:div v]]))
   [:div.fps-wrapper
    [:> FpsView #js {:width 240
                     :height 180}]]])

(defn frame-panel
  []
  (let [ruler-visible? @(rf/subscribe [::ruler.subs/visible?])
        read-only? @(rf/subscribe [::document.subs/read-only?])
        ruler-locked? @(rf/subscribe [::ruler.subs/locked?])
        help-message @(rf/subscribe [::tool.subs/help])
        help-bar @(rf/subscribe [::app.subs/help-bar])
        debug-info? @(rf/subscribe [::app.subs/debug-info])]
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
           {:class "small hidden"
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
       {:data-theme "light"}
       [frame.views/root]
       (when read-only?
         [:div.absolute.inset-0.border-4.border-accent
          (when-let [preview-label @(rf/subscribe [::document.subs/preview-label])]
            [:div.absolute.bg-accent.top-2.left-2.px-1.rounded.text-accent-inverted
             preview-label])])
       (when debug-info? [debug-info])
       (when @(rf/subscribe [::app.subs/backdrop])
         [:div.absolute.inset-0
          {:on-click #(rf/dispatch [::app.events/set-backdrop false])}])
       (when (and help-bar (seq help-message))
         [:div.flex.absolute.justify-center.w-full.pointer-events-none
          {:class "sm:p-4"}
          [:div.bg-primary.overflow-hidden.shadow-lg.w-full
           {:class "sm:rounded-full sm:w-auto"}
           [:div.overlay.text-color.text-xs.gap-1.flex.flex-wrap.py-2.px-4
            {:class "sm:justify-center sm:truncate"
             :aria-live "polite"}
            help-message]]])]]]))

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
        [:> PanelResizeHandle
         {:id "history-resize-handle"
          :className "resize-handle"}]
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
          [:> PanelResizeHandle
           {:id "xml-resize-handle"
            :className "resize-handle"}]
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
       [:> PanelResizeHandle
        {:id "timeline-resize-handle"
         :className "resize-handle"}])
     (when timeline-visible
       [:> Panel
        {:id "timeline-panel"
         :minSize 10
         :defaultSize 20
         :order 2}
        [timeline.views/root]])
     [repl.views/root]]))

(defonce paper-size
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

(defn document-size-select []
  [:> Select/Root
   {:onValueChange #(rf/dispatch [::document.events/new-from-template
                                  (get paper-size %)])}
   [:> Select/Trigger
    {:class "button px-2 overlay rounded-sm"
     :aria-label (t [::select-size "Select size"])}
    [:div.flex.items-center.gap-2
     [:> Select/Value
      {:placeholder (t [::select-template "Select template"])}]
     [:> Select/Icon
      [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:class "menu-content rounded-sm select-content"
      :style {:min-width "auto"}}

     [:> Select/Viewport
      {:class "select-viewport"}
      [:> Select/Group
       [:> Select/Item
        {:value :empty-canvas
         :class "menu-item select-item"}
        [:> Select/ItemText
         (t [::empty-canvas "Empty canvas"])]]
       (for [[k _v] (sort paper-size)]
         ^{:key k}
         [:> Select/Item
          {:value k
           :class "menu-item select-item"}
          [:> Select/ItemText (str "A" k)]])]]]]])

(defn recent-document
  [file-path]
  [:div.flex.items-center.gap-x-2.flex-wrap
   [views/icon "folder"]
   [:button.button-link.text-lg
    {:on-click #(rf/dispatch [::document.events/open file-path])}
    (.basename path file-path)]
   [:span.text-lg.text-muted (.dirname path file-path)]])

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

         [:p.text-xl.text-muted.font-bold
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
           {:on-click #(rf/dispatch [::document.events/open nil])}
           (t [::open "Open"])]
          [views/shortcuts [::document.events/open nil]]]

         (when (seq recent-documents)
           [:<> [:h2.mb-3.mt-8.text-2xl
                 (t [::recent "Recent"])]

            (for [file-path (take 5 recent-documents)]
              ^{:key file-path}
              [recent-document file-path])])

         [:h2.mb-3.mt-8.text-2xl
          (t [::help "Help"])]

         (->> [["command"
                (t [::command-panel "Command panel"])
                [::dialog.events/show-cmdk]]
               ["earth"
                (t [::website "Website"])
                [::events/open-remote-url "https://repath.studio/"]]
               ["commit"
                (t [::source-code "Source Code"])
                [::events/open-remote-url "https://github.com/repath-project/repath-studio"]]
               ["list"
                (t [::changelog "Changelog"])
                [::events/open-remote-url "https://repath.studio/roadmap/changelog/"]]]
              (map #(apply help-command %))
              (into [:div]))]]]]]]])

(defn root
  []
  (let [documents @(rf/subscribe [::document.subs/entities])
        tree-visible @(rf/subscribe [::app.subs/panel-visible? :tree])
        properties-visible @(rf/subscribe [::app.subs/panel-visible? :properties])
        active-tool @(rf/subscribe [::tool.subs/active])
        recent-documents @(rf/subscribe [::document.subs/recent])
        lang-dir @(rf/subscribe [::app.subs/lang-dir])
        is-app-loading @(rf/subscribe [::app.subs/loading?])]
    (when-not is-app-loading
      [:> Direction/Provider {:dir lang-dir}
       [:> Tooltip/Provider
        [:div.flex.flex-col.flex-1.h-dvh.overflow-hidden.justify-between
         [window.views/app-header]
         (if (seq documents)
           [:div.flex.h-full.flex-1.overflow-hidden.gap-px
            (when tree-visible
              [:div.flex-col.hidden.overflow-hidden
               {:class "md:flex"
                :style {:width "227px"}}
               [document.views/actions]
               [tree.views/root]])
            [:div.flex.flex-col.flex-1.overflow-hidden.h-full
             [document.views/tab-bar]
             [:div.flex.h-full.flex-1.gap-px.overflow-hidden
              [:div.flex.h-full.flex-col.flex-1.overflow-hidden.gap-px
               [editor]]
              [:div.flex.gap-px
               (when properties-visible
                 [:div.hidden
                  {:class "md:flex"}
                  [:div.flex.flex-col.h-full.w-80
                   [views/scroll-area
                    (tool.hierarchy/right-panel active-tool)]
                   [:div.bg-primary.grow.flex]]])
               [:div.bg-primary.flex
                [views/scroll-area [toolbar.object/root]]]]]]]
           [home recent-documents])
         [:div]]
        [dialog.views/root]
        [notification.views/main]]])))
