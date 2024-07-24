(ns renderer.snap.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.snap.db :as snap.db]
   [renderer.snap.events :as-alias snap.e]
   [renderer.snap.subs :as-alias snap.s]))

(defn options-dropdown
  []
  (let [options @(rf/subscribe [::snap.s/options])]
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
       (for [option (rest snap.db/options)]
         ^{:key option}
         [:> DropdownMenu/CheckboxItem
          {:class "menu-checkbox-item inset"
           :on-click #(.stopPropagation %)
           :onSelect #(do (.preventDefault %)
                          (rf/dispatch [::snap.e/toggle-option option]))
           :checked (contains? options option)}
          [:> DropdownMenu/ItemIndicator
           {:class "menu-item-indicator"}
           [comp/icon "checkmark"]]
          (name option)])]]]))

(defn root
  []
  [:button.icon-button.items-center.px-1.gap-1
   {:title "Snap"
    :class (when @(rf/subscribe [::snap.s/enabled?]) "selected")
    :style {:margin-right 0
            :width "auto"
            :display "flex"}
    :on-click #(rf/dispatch [::snap.e/toggle])}
   [renderer.components/icon "magnet"]
   [options-dropdown]])
