(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.ui :as ui]))

(defn button
  [tool]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        primary-tool @(rf/subscribe [::tool.subs/primary])
        active (= active-tool tool)
        primary (= primary-tool tool)
        properties (tool.hierarchy/properties tool)
        label (or (:label properties) (string/capitalize (name tool)))]
    (when (:icon properties)
      [:> Tooltip/Root
       [:> Tooltip/Trigger {:as-child true}
        [:span
         [ui/radio-icon-button (:icon properties) active
          {:class (when primary "outline outline-offset-[-1px] outline-accent")
           :aria-label (str "activate " label)
           :on-click #(rf/dispatch [::tool.events/activate tool])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 5
          :side "top"}
         [:div.flex.gap-2.items-center
          (or (:label properties)
              (string/capitalize (name tool)))]]]])))

(defn group
  [items]
  (into [:div.flex.gap-1]
        (map button items)))

(def groups
  [[:transform :edit :pan :zoom]
   [:svg]
   [:circle :ellipse :rect :line :polyline :polygon :image :text]
   [:blob]
   [:brush :pen]
   [:dropper :fill :measure]])

(defn root
  []
  (into [:div.justify-center.bg-primary.toolbar]
        (interpose [:span.v-divider]
                   (map group groups))))
