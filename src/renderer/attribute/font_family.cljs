(ns renderer.attribute.font-family
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/font-family"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["cmdk" :as Command]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.subs :as-alias app.s]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]
   [renderer.ui :as ui]))

(defmethod hierarchy/description [:default :font-family]
  []
  "The font-family attribute indicates which font family will be used to render the text,
   specified as a prioritized list of font family names and/or generic family names.")

(defn suggestions-list
  [suggestions]
  [:div.flex.flex-col
   [:> Command/Command
    {:label "Command Menu"
     :on-key-down #(when-not (= (.-key %) "Escape") (.stopPropagation %))}
    [:> Command/CommandInput
     {:class "p-2 text-sm bg-secondary border-b border-default"
      :placeholder "Search for a font"}]
    [ui/scroll-area
     [:> Command/CommandList
      {:class "p-1"}
      [:> Command/CommandEmpty
       "No local fonts found."]
      (for [item suggestions]
        ^{:key item}
        [:> Command/CommandItem
         {:on-select #(rf/dispatch [::element.e/set-attr :font-family item])}
         item])]]]])

(defmethod hierarchy/form-element [:default :font-family]
  [_ k v disabled?]
  (let [suggestions @(rf/subscribe [::app.s/font-options])]
    [:<>
     [v/form-input
      {:key k
       :value v
       :disabled? disabled?}]
     [:> Popover/Root {:modal true
                       :onOpenChange (fn [state]
                                       (when (and state (empty? suggestions))
                                         (rf/dispatch-sync [::app.e/load-system-fonts])))}
      [:> Popover/Trigger {:asChild true}
       [:button.ml-px.inline-block.bg-primary.text-muted.h-full
        {:style {:flex "0 0 26px"}}
        [ui/icon "magnifier" {:class "icon small"}]]]
      [:> Popover/Portal
       [:> Popover/Content {:sideOffset 5
                            :className "popover-content"
                            :align "end"}
        [suggestions-list suggestions]
        [:> Popover/Arrow {:class "popover-arrow"}]]]]]))
