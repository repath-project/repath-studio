(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn button
  [tool]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        cached-tool @(rf/subscribe [::tool.subs/cached])
        active (= active-tool tool)
        primary (= cached-tool tool)
        properties (tool.hierarchy/properties tool)
        label (or (:label properties)
                  (string/capitalize (name tool)))]
    (when (:icon properties)
      [:> Tooltip/Root
       [:> Tooltip/Trigger
        {:as-child true}
        [:span
         [views/radio-icon-button (:icon properties) active
          {:class (when primary "outline outline-inset outline-accent")
           :aria-label label
           :on-click #(rf/dispatch [::tool.events/activate tool])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 10
          :side "top"
          :on-escape-key-down #(.stopPropagation %)}
         [:div.flex.gap-2.items-center
          label
          [views/shortcuts [::tool.events/activate tool]]]]]])))

(defn group
  [items]
  (into [:div.flex.flex-wrap.justify-center.md:gap-1.flex-nowrap
         {:class "gap-0.5"}]
        (map button items)))

(defn groups
  []
  (let [dropper? @(rf/subscribe [::app.subs/feature? :eye-dropper])]
    [[:transform :edit :pan :zoom]
     [:svg]
     [:circle :ellipse :rect :line :polyline :polygon :image :text]
     [:brush :pen]
     (cond-> [:fill :measure]
       dropper? (conj :dropper))]))

(defn root
  []
  (let [sm? @(rf/subscribe [::window.subs/breakpoint? :sm])]
    (->> (map group (groups))
         (interpose [:span.v-divider {:class (when-not sm? "mx-0")}])
         (into [:div.justify-center.bg-primary.toolbar]))))
