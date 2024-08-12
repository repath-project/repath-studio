(ns renderer.attribute.font-family
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/font-family"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["cmdk" :as Command]
   [re-frame.core :as rf]
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
  [:div.flex.flex-col.p-1
   [:> Command/Command
    {:label "Command Menu"
     :on-key-down #(when-not (= (.-key %) "Escape") (.stopPropagation %))}
    [:> Command/CommandInput
     {:placeholder "Search for a font"}]
    [:> Command/CommandList
     [:> Command/CommandEmpty
      "No fonts found."]
     (for [item suggestions]
       ^{:key item}
       [:> Command/CommandItem
        {:key key
         :on-select #(rf/dispatch [::element.e/set-attr :font-family item])}
        item])]]])

(defmethod hierarchy/form-element [:default :font-family]
  [_ k v disabled?]
  (let [suggestions @(rf/subscribe [:font-options])]
    [:<>
     [v/form-input
      {:key k
       :value v
       :disabled? disabled?}]
     (when (seq suggestions)
       [:> Popover/Root {:modal true}
        [:> Popover/Trigger {:asChild true}
         [:button.ml-px.inline-block.bg-primary.text-muted.h-full
          {:style {:flex "0 0 26px"}}
          [ui/icon "magnifier" {:class "icon small"}]]]
        [:> Popover/Portal
         [:> Popover/Content {:sideOffset 5
                              :className "popover-content"
                              :align "end"}
          [suggestions-list suggestions]
          [:> Popover/Arrow {:class "popover-arrow"}]]]])]))
