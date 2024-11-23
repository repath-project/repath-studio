(ns renderer.app.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["@radix-ui/react-tooltip" :as Tooltip]
   ["path-browserify" :as path]
   ["react-resizable-panels" :refer [Panel PanelGroup PanelResizeHandle]]
   [clojure.string :as str]
   [config :as config]
   [re-frame.core :as rf]
   [renderer.app.events :as e]
   [renderer.app.subs :as-alias app.s]
   [renderer.codemirror.views :as cm.v]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.dialog.views :as dialog.v]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.document.views :as document.v]
   [renderer.element.subs :as-alias element.s]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.frame.views :as frame.v]
   [renderer.history.views :as history.v]
   [renderer.notification.views :as notification]
   [renderer.reepl.views :as repl.v]
   [renderer.ruler.events :as-alias ruler.e]
   [renderer.ruler.subs :as-alias ruler.s]
   [renderer.ruler.views :as ruler.v]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.timeline.views :as timeline.v]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.toolbar.object :as toolbar.object]
   [renderer.toolbar.status :as toolbar.status]
   [renderer.toolbar.tools :as toolbar.tools]
   [renderer.tree.views :as tree.v]
   [renderer.ui :as ui]
   [renderer.window.events :as-alias window.e]
   [renderer.window.views :as window.v]))

(defn coll->str
  [coll]
  (str "[" (str/join " " (map #(.toFixed % 2) coll)) "]"))

(defn map->str
  [m]
  (interpose ", " (map (fn [[k v]]
                         ^{:key k}
                         [:span (str (name k)  ": " (if (number? v)
                                                      (.toFixed v 2)
                                                      (coll->str v)))]) m)))

(defn debug-rows
  []
  [["Dom rect" (map->str @(rf/subscribe [::app.s/dom-rect]))]
   ["Viewbox" (coll->str @(rf/subscribe [::frame.s/viewbox]))]
   ["Pointer position" (coll->str @(rf/subscribe [::app.s/pointer-pos]))]
   ["Adjusted pointer position" (coll->str @(rf/subscribe [::app.s/adjusted-pointer-pos]))]
   ["Pointer offset" (coll->str @(rf/subscribe [::app.s/pointer-offset]))]
   ["Adjusted pointer offset" (coll->str @(rf/subscribe [::app.s/adjusted-pointer-offset]))]
   ["Pointer drag?" (str @(rf/subscribe [::tool.s/drag]))]
   ["Pan" (coll->str @(rf/subscribe [::document.s/pan]))]
   ["Active tool" @(rf/subscribe [::tool.s/active])]
   ["Primary tool" @(rf/subscribe [::tool.s/primary])]
   ["State"  @(rf/subscribe [::tool.s/state])]
   ["Clicked element" (:id @(rf/subscribe [::app.s/clicked-element]))]
   ["Ignored elements" @(rf/subscribe [::document.s/ignored-ids])]
   ["Snap" (map->str @(rf/subscribe [::snap.s/nearest-neighbor]))]])

(defn debug-info
  []
  (into [:div.absolute.top-1.left-2.pointer-events-none
         {:style {:color "#555"}}]
        (for [[s v] (debug-rows)]
          [:div.flex [:strong.mr-1 s] [:div v]])))

(defn frame-panel
  []
  (let [ruler-visible @(rf/subscribe [::ruler.s/visible])
        read-only @(rf/subscribe [::document.s/read-only])
        ruler-size @(rf/subscribe [::ruler.s/size])
        ruler-locked @(rf/subscribe [::ruler.s/locked])]
    [:div.flex.flex-col.flex-1.h-full.gap-px
     [:div
      [ui/scroll-area [toolbar.tools/root]]
      (when ruler-visible
        [:div.flex.gap-px
         [:div.bg-primary
          {:style {:width ruler-size
                   :height ruler-size}}
          [ui/icon-button
           (if ruler-locked "lock" "unlock")
           {:class "small hidden"
            :title (if ruler-locked "unlock" "lock")
            :on-click #(rf/dispatch [::ruler.e/toggle-locked])}]]
         [:div.bg-primary.flex-1
          [ruler.v/ruler :horizontal]]])]
     [:div.flex.flex-1.relative.gap-px
      (when ruler-visible
        [:div.bg-primary
         [ruler.v/ruler :vertical]])
      [:div.relative.grow.flex
       [frame.v/root]
       (if read-only
         [:div.absolute.inset-0.border-4.border-accent]
         (when @(rf/subscribe [::app.s/debug-info])
           [debug-info]))
       (when @(rf/subscribe [::app.s/backdrop])
         [:div.absolute.inset-0
          {:on-click #(rf/dispatch [::e/set-backdrop false])}])]]]))

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
     (when @(rf/subscribe [::app.s/panel-visible :history])
       [:<>
        [:> PanelResizeHandle
         {:id "history-resize-handle"
          :className "resize-handle"}]
        [:> Panel {:id "history-panel"
                   :defaultSize 30
                   :minSize 5
                   :order 2}
         [:div.bg-primary.h-full
          [history.v/root]]]])

     (when @(rf/subscribe [::app.s/panel-visible :xml])
       (let [xml @(rf/subscribe [::element.s/xml])]
         [:<>
          [:> PanelResizeHandle
           {:id "xml-resize-handle"
            :className "resize-handle"}]
          [:> Panel {:id "xml-panel"
                     :defaultSize 30
                     :minSize 5
                     :order 3}

           [:div.h-full.bg-primary.flex
            [ui/scroll-area
             [:div.p-1
              [cm.v/editor xml
               {:options {:mode "text/xml"
                          :readOnly true}}]]]]]]))]]])

(defn editor
  []
  (let [timeline-visible @(rf/subscribe [::app.s/panel-visible :timeline])]
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
        [timeline.v/root]])
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

(defn home [recent-documents]
  [:div.flex.overflow-hidden
   [ui/scroll-area
    [:div.flex.justify-center.p-2
     [:div.justify-between.flex.w-full.lg:w-auto
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
           {:class "button px-2 overlay rounded"
            :aria-label "Select size"}
           [:div.flex.items-center.gap-2
            [:> Select/Value {:placeholder "Select template"}]
            [:> Select/Icon
             [ui/icon "chevron-down"]]]]
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
          {:on-click #(rf/dispatch [::document.e/open nil])}
          "Open"]
         [ui/shortcuts [::document.e/open nil]]]

        (when (seq recent-documents)
          [:<> [:h2.mb-3.mt-8.text-2xl "Recent"]

           (for [file-path (take 5 recent-documents)]
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
        [:img {:src "./img/icon.svg"}]]]]]]])

(defn root
  []
  (let [documents (rf/subscribe [::document.s/entities])
        tree-visible (rf/subscribe [::app.s/panel-visible :tree])
        properties-visible (rf/subscribe [::app.s/panel-visible :properties])
        active-tool (rf/subscribe [::tool.s/active])
        recent-docs (rf/subscribe [::document.s/recent])]
    [:> Tooltip/Provider
     [:div.flex.flex-col.flex-1.h-dvh.overflow-hidden.justify-between
      [window.v/app-header]
      (if (seq @documents)
        [:div.flex.h-full.flex-1.overflow-hidden.gap-px
         (when @tree-visible
           [:div.flex-col.hidden.md:flex.overflow-hidden
            {:style {:width "227px"}}
            [document.v/actions]
            [tree.v/root]])
         [:div.flex.flex-col.flex-1.overflow-hidden.h-full
          [document.v/tab-bar]
          [:div.flex.h-full.flex-1.gap-px.overflow-hidden
           [:div.flex.h-full.flex-col.flex-1.overflow-hidden
            [editor]]
           [:div.flex
            (when @properties-visible
              [:div.hidden.md:flex
               [:div.flex.flex-col.h-full.w-80
                [ui/scroll-area
                 (tool.hierarchy/right-panel @active-tool)]
                [:div.bg-primary.grow.flex.mr-px]]])
            [:div.bg-primary.flex
             [ui/scroll-area [toolbar.object/root]]]]]]]
        [home @recent-docs])
      [:div]]
     [dialog.v/root]
     [notification/main]]))
