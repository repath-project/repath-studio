(ns renderer.snap.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.subs]
   [renderer.snap.db :as snap.db]
   [renderer.snap.events :as-alias snap.events]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]))

(defn options-dropdown
  []
  (let [options @(rf/subscribe [::snap.subs/options])]
    [:> DropdownMenu/Root
     [:> DropdownMenu/Trigger
      {:as-child true}
      [:div.h-full.flex.items-center
       {:role "button"
        :title (t [::snap-options "Snap options"])
        :class "hover:pb-1"}
       [views/icon "chevron-up"]]]
     [:> DropdownMenu/Portal
      [:> DropdownMenu/Content
       {:side "top"
        :align "end"
        :sideOffset 5
        :alignOffset -5
        :position "popper"
        :class "menu-content rounded-sm select-content"
        :on-key-down #(.stopPropagation %)
        :on-escape-key-down #(.stopPropagation %)}
       (for [option snap.db/snap-options]
         ^{:key option}
         [:> DropdownMenu/CheckboxItem
          {:class "menu-checkbox-item inset"
           :on-click #(.stopPropagation %)
           :onSelect #(do (.preventDefault %)
                          (rf/dispatch [::snap.events/toggle-option option]))
           :checked (contains? options option)}
          [:> DropdownMenu/ItemIndicator
           {:class "menu-item-indicator"}
           [views/icon "checkmark"]]
          (t [(keyword "renderer.snap.views" (name option)) (name option)])])]]]))

(defn root
  []
  [:button.icon-button.items-center.px-1.gap-1.w-auto.flex
   {:title "Snap"
    :class (when @(rf/subscribe [::snap.subs/active?]) "accent")
    :on-click #(rf/dispatch [::snap.events/toggle])}
   [views/icon "magnet"]
   [options-dropdown]])

(defn canvas-label
  [nearest-neighbor]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        point-label (-> nearest-neighbor meta :label)
        base-label (-> nearest-neighbor :base-point meta :label)
        label (string/join (t [::to " to "])
                           (remove nil? [base-label point-label]))
        point (:point nearest-neighbor)]
    [:<>
     [utils.svg/times point]
     (when (not-empty label)
       [utils.svg/label label (matrix/add point margin) "start"])]))
