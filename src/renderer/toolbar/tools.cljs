(ns renderer.toolbar.tools
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.tool.events :as-alias tool.events]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.subs :as-alias tool.subs]
   [renderer.utils.i18n :refer [t]]
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
        [views/radio-icon-button (:icon properties) active
         {:class (when primary "outline outline-inset outline-accent")
          :aria-label label
          :on-click #(rf/dispatch [::tool.events/activate tool])}]]
       [:> Tooltip/Portal
        [:> Tooltip/Content
         {:class "tooltip-content"
          :sideOffset 10
          :side "top"
          :on-escape-key-down #(.stopPropagation %)}
         [:div.flex.gap-2.items-center
          label
          [views/shortcuts [::tool.events/activate tool]]]]]])))

(defn button-group
  [items]
  (into [:div.flex.justify-center.md:gap-1
         {:class "gap-0.5"}]
        (map button items)))

(defn dropdown-button
  [{:keys [label tools]}]
  (let [active-tool @(rf/subscribe [::tool.subs/active])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])
        top-tool (if (some #{active-tool} tools)
                   active-tool
                   (first tools))]
    [:div.button-group
     [button top-tool]
     (when (second tools)
       [:> DropdownMenu/Root
        [:> DropdownMenu/Trigger
         {:as-child true}
         [:button.button.flex.items-center.justify-center.px-2.font-mono
          {:aria-label label}
          [views/icon "chevron-down"]]]
        [:> DropdownMenu/Portal
         (->> tools
              (mapv (fn [tool]
                      (let [properties (tool.hierarchy/properties tool)]
                        {:label (:label properties)
                         :action [::tool.events/activate tool]
                         :icon (:icon properties)})))
              (into [:> DropdownMenu/Content
                     {:side "bottom"
                      :align-offset (if md? -33 -35)
                      :align "start"
                      :class "menu-content rounded-sm"
                      :on-key-down #(.stopPropagation %)
                      :on-escape-key-down #(.stopPropagation %)}
                     [:> DropdownMenu/Arrow {:class "fill-primary"}]]
                    (map views/dropdown-menu-item)))]])]))

(defn groups []
  [{:id :transform
    :label (t [::transform "Transform tools"])
    :tools [:transform :edit :pan :zoom]}
   {:id :containers
    :label (t [::containers "Container tools"])
    :tools [:svg]}
   {:id :shapes
    :label (t [::shapes "Shape tools"])
    :tools [:circle :ellipse :rect :line :polyline :polygon :image :text]}
   {:id :drawing
    :label (t [::drawing "Drawing tools"])
    :tools [:brush :pen]}
   {:id :misc
    :label (t [::misc "Miscallaneous tools"])
    :tools (cond-> [:fill :measure]
             @(rf/subscribe [::app.subs/feature? :eye-dropper])
             (update :tools conj :dropper))}])

(defn root
  []
  (let [xl @(rf/subscribe [::window.subs/breakpoint? :xl])]
    (if xl
      (->> (groups)
           (map :tools)
           (map button-group)
           (interpose [:span.v-divider])
           (into [:div.justify-center.bg-primary.toolbar]))
      (->> (groups)
           (map dropdown-button)
           (into [:div.bg-primary.toolbar.justify-center.py-2])))))
