(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.subs :as-alias app.s]
   [renderer.tool.base :as tool]
   [renderer.ui :as ui]))

(defn button
  [tool]
  (let [active-tool @(rf/subscribe [::app.s/tool])
        primary-tool @(rf/subscribe [::app.s/primary-tool])
        selected? (= active-tool tool)
        primary? (= primary-tool tool)]
    (when (:icon (tool/properties tool))
      [:> Tooltip/Root
       [:> Tooltip/Trigger {:asChild true}
        [:span
         [ui/radio-icon-button (:icon (tool/properties tool)) selected?
          {:class (when primary? "outline-shadow")
           :on-click #(rf/dispatch [::app.e/set-tool tool])}]]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 5
          :side "top"}
         [:div.flex.gap-2.items-center
          (str/capitalize (name tool))]]]])))

(defn group
  [items]
  (into [:div.flex.gap-1]
        (map button items)))

(def groups
  [[:select :edit :pan :zoom]
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
