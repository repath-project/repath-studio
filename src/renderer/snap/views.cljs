(ns renderer.snap.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn options-dropdown
  []
  (let [options @(rf/subscribe [:snap/options])]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:aria-label "Snap"
       :as-child true
       :style {:background "transparent"
               :width "16px"
               :margin 0}}
      [:div.h-full.hover:pb-1.flex.items-center
       [comp/icon "chevron-up" {:class "icon small"}]]]
     [:> DropdownMenu/Portal
      [:> DropdownMenu/Content
       {:side "top"
        :align "end"
        :sideOffset 5
        :alignOffset -5
        :position "popper"
        :class "menu-content rounded select-content"}
       (for [option #{:centers :midpoints :corners :nodes}]
         ^{:key option}
         [:> DropdownMenu/CheckboxItem
          {:class "menu-checkbox-item inset"
           :on-click #(.stopPropagation %)
           :onSelect #(do (.preventDefault %)
                          (rf/dispatch [:element/import-traced-image option]))
           :checked (contains? options option)}
          [:> DropdownMenu/ItemIndicator
           {:class "menu-item-indicator"}
           [comp/icon "checkmark"]]
          (name option)])]]]))

(defn root
  []
  [:button.icon-button.items-center.px-1.gap-1
   {:title "Snap"
    :class (when @(rf/subscribe [:snap/enabled?]) "selected")
    :style {:margin-right 0
            :width "auto"
            :display "flex"}
    :on-click #(rf/dispatch [:snap/toggle])}
   [renderer.components/icon "magnet"]
   [options-dropdown]])
