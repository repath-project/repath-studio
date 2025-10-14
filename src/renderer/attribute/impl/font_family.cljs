(ns renderer.attribute.impl.font-family
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/font-family"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["cmdk" :as Command]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.element.events :as-alias element.events]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [:default :font-family]
  []
  (t [::description
      "The font-family attribute indicates which font family
       will be used to render the text,
       specified as a prioritized list of font
       family names and/or generic family names."]))

(defn font-item
  [font]
  [:> Command/CommandItem
   {:on-select #(rf/dispatch [::element.events/set-attr :font-family font])}
   [:div.flex.justify-between.items-center.w-full.gap-2
    [:div font]
    [:div.leading-none.text-foreground-muted
     {:style {:font-family font}}
     "AaBbCc 0123"]]])

(defn suggestions-list
  [font-list]
  [:div.flex.flex-col
   [:> Command/Command
    {:label "Command Menu"
     :on-key-down #(.stopPropagation %)}
    [:> Command/CommandInput
     {:class "p-3 bg-primary border-b border-border w-full"
      :placeholder (t [::search-font "Search for a font"])}]
    [views/scroll-area
     [:> Command/CommandList
      {:class "p-1"}
      [:> Command/CommandEmpty
       (if-not font-list
         [:div.w-full [views/loading-indicator]]
         (t [::no-local-font "No local fonts found."]))]
      (for [font font-list]
        ^{:key font}
        [font-item font])]]]])

(defmethod attribute.hierarchy/form-element [:default :font-family]
  [_ k v attrs]
  (let [font-list @(rf/subscribe [::app.subs/font-list])
        local-fonts @(rf/subscribe [::app.subs/feature? :local-fonts])]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k v attrs]
     (when local-fonts
       [:> Popover/Root
        {:modal true
         :onOpenChange (fn [state]
                         (when (and state (empty? font-list))
                           (rf/dispatch [::app.events/load-system-fonts])))}
        [:> Popover/Trigger
         {:title (t [::select-font "Select font"])
          :class "form-control-button"
          :disabled (:disabled attrs)}
         [views/icon "magnifier"]]
        [:> Popover/Portal
         [:> Popover/Content
          {:sideOffset 5
           :class "popover-content"
           :align "end"}
          [suggestions-list font-list]
          [:> Popover/Arrow {:class "fill-primary"}]]]])]))
