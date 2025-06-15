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
   [renderer.views :as views]))

(defmethod attribute.hierarchy/description [:default :font-family]
  []
  "The font-family attribute indicates which font family will be used to render the text,
   specified as a prioritized list of font family names and/or generic family names.")

(defn font-item
  [font]
  [:> Command/CommandItem
   {:on-select #(rf/dispatch [::element.events/set-attr :font-family font])}
   [:div.flex.justify-between.items-center.w-full.gap-2
    [:div font]
    [:div.leading-none.text-muted
     {:style {:font-family font}}
     "AaBbCc 0123"]]])

(defn suggestions-list
  [font-list]
  [:div.flex.flex-col
   [:> Command/Command
    {:label "Command Menu"
     :on-key-down #(.stopPropagation %)}
    [:> Command/CommandInput
     {:class "p-2 text-sm bg-secondary border-b border-default"
      :placeholder "Search for a font"}]
    [views/scroll-area
     [:> Command/CommandList
      {:class "p-1"}
      [:> Command/CommandEmpty
       (if-not font-list
         [:div.w-full [views/loading-indicator]]
         "No local fonts found.")]
      (for [font font-list]
        ^{:key font}
        [font-item font])]]]])

(defmethod attribute.hierarchy/form-element [:default :font-family]
  [_ k v attrs]
  (let [font-list @(rf/subscribe [::app.subs/font-list])]
    [:div.flex.gap-px.w-full
     [attribute.views/form-input k v attrs]
     [:> Popover/Root
      {:modal true
       :onOpenChange (fn [state]
                       (when (and state (empty? font-list))
                         (rf/dispatch [::app.events/load-system-fonts])))}
      [:> Popover/Trigger
       {:title "Select font"
        :class "form-control-button"
        :disabled (:disabled attrs)}
       [views/icon "magnifier"]]
      [:> Popover/Portal
       [:> Popover/Content
        {:sideOffset 5
         :className "popover-content"
         :align "end"}
        [suggestions-list font-list]
        [:> Popover/Arrow {:class "popover-arrow"}]]]]]))
