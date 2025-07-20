(ns renderer.history.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["react" :as react]
   ["react-d3-tree" :refer [Tree]]
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.dialog.events :as-alias dialog.events]
   [renderer.history.events :as-alias history.events]
   [renderer.history.subs :as-alias history.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defn select-option
  [{:keys [id explanation]}]
  [:> Select/Item
   {:value (str id)
    :class "menu-item select-item"}
   [:> Select/ItemText (if (string? explanation)
                         explanation
                         (explanation))]])

(defn select
  [label options disabled?]
  [:> Select/Root
   {:onValueChange #(rf/dispatch [::history.events/go-to (uuid %)])
    :disabled disabled?}
   [:> Select/Trigger
    {:aria-label label
     :as-child true}
    [:div.w-4.m-0.bg-transparent.h-full.flex.items-center
     {:class "hover:pt-1"}
     [:> Select/Value ""]
     [:> Select/Icon
      [views/icon "chevron-down"]]]]
   [:> Select/Portal
    [:> Select/Content
     {:side "top"
      :sideOffset 5
      :alignOffset -24
      :position "popper"
      :class "menu-content rounded-sm select-content"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     [:> Select/ScrollUpButton {:class "select-scroll-button"}
      [views/icon "chevron-up"]]
     [:> Select/Viewport {:class "select-viewport"}
      (into [:> Select/Group]
            (map select-option options))]
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [views/icon "chevron-down"]]]]])

(defn node
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/_src_tree_types_.treeprops.html#rendercustomnodeelement"
  [^js/CustomNodeElementProps props]
  (let [datum (.-nodeDatum props)
        active? (.-active datum)
        id (uuid (.-id datum))
        color (if active? "var(--color-accent)" (.-color datum))]
    (reagent/as-element
     [:circle.transition-fill
      {:class "hover:stroke-accent"
       :on-click #(rf/dispatch [::history.events/go-to id])
       :on-pointer-enter #(when-not active? (rf/dispatch [::history.events/preview id]))
       :on-pointer-leave #(rf/dispatch [::history.events/reset-state id])
       :cx "0"
       :cy "0"
       :stroke color
       :stroke-width 4
       :fill color
       :r 18}
      [:title (.-name datum)]])))

(defn on-update
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/_src_tree_types_.treeprops.html#onupdate"
  [target]
  (let [translate (.-translate target)
        zoom (.-zoom target)]
    (rf/dispatch-sync [::history.events/tree-view-updated zoom [(.-x translate)
                                                                (.-y translate)]])))

(defn center
  [ref]
  (when-let [current (.-current ref)]
    (matrix/div [(.-clientWidth current)
                 (.-clientHeight current)] 2)))

(defn tree
  [ref]
  (let [tree-data @(rf/subscribe [::history.subs/tree-data])
        zoom @(rf/subscribe [::history.subs/zoom])
        dom-el (.-current ref)
        [x y] @(rf/subscribe [::history.subs/translate])
        translate #js {:x (or x (when dom-el (/ (.-clientWidth dom-el) 2)))
                       :y (or y (when dom-el (/ (.-clientHeight dom-el) 2)))}]
    [:> Tree
     {:data tree-data
      :collapsible false
      :orientation "vertical"
      :rootNodeClassName "root-node"
      :depthFactor -100
      :zoom zoom
      :translate translate
      :onUpdate on-update
      :separation #js {:nonSiblings 1 :siblings 1}
      :renderCustomNodeElement node}]))

(defn root
  []
  (let [ref (react/createRef)]
    [:div.flex.flex-col.h-full
     [:div.flex.p-1
      [:button.button.flex-1
       {:on-click #(rf/dispatch [::history.events/tree-view-updated 0.5 (center ref)])}
       (t [::center-view "Center view"])]
      [:button.button.flex-1
       {:on-click #(rf/dispatch [::dialog.events/show-confirmation
                                 {:title (t [::action-cannot-undone
                                             "This action cannot be undone."])
                                  :description (t [::clear-history-description
                                                   "Are you sure you wish to clear the
                                                    document history?"])
                                  :confirm-label (t [::clear-history "Clear history"])
                                  :action [::history.events/clear]}])}
       (t [::clear-history "Clear history"])]]
     [:div.flex-1 {:ref ref}
      [tree ref]]]))
