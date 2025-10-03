(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as string]
   [re-frame.core :as rf]
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
          {:class (when primary "outline outline-offset-[-1px] outline-accent")
           :aria-label label
           :on-click #(rf/dispatch [::tool.events/activate tool])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 10
          :side "top"}
         [:div.flex.gap-2.items-center
          label
          [views/shortcuts [::tool.events/activate tool]]]]]])))

(defn group
  [items]
  (into [:div.flex.gap-1]
        (map button items)))

(defn groups
  [sm?]
  (if sm?
    [[:transform :edit :pan :zoom]
     [:svg]
     [:circle :ellipse :rect :line :polyline :polygon :image :text]
     [:blob]
     [:brush :pen]
     [:dropper :fill :measure]]
    [[:transform :edit :pan :zoom]
     [:brush :pen]
     [:dropper :fill :measure]
     [:svg]
     [:circle :ellipse :rect :line :polyline :polygon :image :text]]))

(defn root
  []
  (let [sm? @(rf/subscribe [::window.subs/breakpoint? :sm])]
    (cond->> (map group (groups sm?))
      sm?
      (interpose [:span.v-divider])

      :always
      (into [:div.justify-center.bg-primary.toolbar
             {:class [(when-not sm? "flex-wrap")]}]))))
