(ns renderer.snap.views
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.subs :as-alias document.subs]
   [renderer.snap.events :as-alias snap.events]
   [renderer.snap.subs :as-alias snap.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.svg :as utils.svg]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn menu-option
  [{:keys [id label]} is-checked]
  [:> DropdownMenu/CheckboxItem
   {:class "menu-checkbox-item inset"
    :on-click #(.stopPropagation %)
    :onSelect #(do (.preventDefault %)
                   (rf/dispatch [::snap.events/toggle-option id]))
    :checked is-checked}
   [:> DropdownMenu/ItemIndicator
    {:class "menu-item-indicator"}
    [views/icon "checkmark"]]
   label])

(defn snap-options
  []
  [{:id :centers
    :label (t [::centers "centers"])}
   {:id :midpoints
    :label (t [::midpoints "midpoints"])}
   {:id :corners
    :label (t [::corners "corners"])}
   {:id :nodes
    :label (t [::nodes "nodes"])}])

(defn root
  []
  (let [active? (rf/subscribe [::snap.subs/active?])
        md? @(rf/subscribe [::window.subs/breakpoint? :md])]
    (reagent/with-let [open (reagent/atom false)]
      [:button.button.rounded-sm.items-center.gap-1.w-auto.flex
       {:title (t [::snap "Snap"])
        :class ["active:bg-overlay"
                (if md? "px-1" "px-2")
                (when @active? "accent")
                (when @open "bg-overlay!")]
        :on-click #(rf/dispatch [::snap.events/toggle])}
       [views/icon "magnet"]
       (when md?
         (let [options @(rf/subscribe [::snap.subs/options])]
           [:> DropdownMenu/Root
            {:on-open-change #(reset! open %)}
            [:> DropdownMenu/Trigger
             {:as-child true}
             [:div.h-full.flex.items-center.hover:pb-1
              {:class "min-h-[inherit]"
               :role "button"
               :title (t [::snap-options "Snap options"])}
              [views/icon "chevron-up"]]]
            [:> DropdownMenu/Portal
             (->> (snap-options)
                  (map #(menu-option % (contains? options (:id %))))
                  (into [:> DropdownMenu/Content
                         {:side "top"
                          :align "end"
                          :sideOffset 5
                          :alignOffset -5
                          :position "popper"
                          :class "menu-content rounded-sm select-content"
                          :on-key-down #(.stopPropagation %)
                          :on-escape-key-down #(.stopPropagation %)}
                         [:> DropdownMenu/Arrow
                          {:class "fill-primary"}]]))]]))])))

(defn get-label [label]
  (when label
    (if (string? label)
      label
      (label))))

(defn canvas-label
  [nearest-neighbor]
  (let [zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)
        point-label (-> nearest-neighbor meta :label)
        base-label (-> nearest-neighbor :base-point meta :label)
        point (:point nearest-neighbor)
        [x y] (matrix/add point margin)
        label (->> [base-label point-label]
                   (keep get-label)
                   (string/join (t [::to " to "])))]
    [:<>
     [utils.svg/times point]
     (when (not-empty label)
       [utils.svg/label label {:x x
                               :y y
                               :text-anchor "start"
                               :font-family "var(--font-sans)"}])]))
