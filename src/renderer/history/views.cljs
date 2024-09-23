(ns renderer.history.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["react" :as react]
   ["react-d3-tree" :refer [Tree]]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.history.events :as-alias history.e]
   [renderer.history.subs :as-alias history.s]
   [renderer.ui :as ui]))

(defn select-options
  [history-list]
  (for [{:keys [id explanation]} history-list]
    ^{:key id}
    [:> Select/Item
     {:value (str id)
      :class "menu-item select-item"}
     [:> Select/ItemText explanation]]))

(defn select
  [label options disabled?]
  [:> Select/Root
   {:onValueChange #(rf/dispatch [::history.e/move (uuid %)])
    :disabled disabled?}
   [:> Select/Trigger
    {:aria-label label
     :as-child true
     :style {:background "transparent"
             :width "16px"
             :margin 0}}
    [:div.h-full.hover:pt-1.flex.items-center
     [:> Select/Value ""]
     [:> Select/Icon
      [ui/icon "chevron-down" {:class "small"}]]]]
   [:> Select/Portal
    [:> Select/Content
     {:side "top"
      :sideOffset 5
      :alignOffset -24
      :position "popper"
      :class "menu-content rounded select-content"}
     [:> Select/ScrollUpButton {:class "select-scroll-button"}
      [ui/icon "chevron-up"]]
     [:> Select/Viewport {:class "select-viewport"}
      [:> Select/Group
       (select-options options)]]
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [ui/icon "chevron-down"]]]]])

(defn node
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/_src_tree_types_.treeprops.html#rendercustomnodeelement"
  [^js/CustomNodeElementProps props]
  (let [datum (.-nodeDatum props)
        id (uuid (.-id datum))
        color (if (.-active datum) "var(--accent)" (.-color datum))]
    (ra/as-element
     [:circle
      {:on-click #(rf/dispatch [::history.e/move id])
       :on-pointer-enter #(rf/dispatch [::history.e/preview id])
       :on-pointer-leave #(rf/dispatch [::history.e/swap id])
       :cx "0"
       :cy "0"
       :stroke color
       :stroke-width 4
       :r 18
       :ref #(when % (.setAttribute % "fill" color))}
      [:title (.-name datum)]])))

(defn on-update
  "https://bkrem.github.io/react-d3-tree/docs/interfaces/_src_tree_types_.treeprops.html#onupdate"
  [target]
  (let [translate (.-translate target)
        zoom (.-zoom target)]
    (rf/dispatch [::history.e/tree-view-updated zoom [(.-x translate)
                                                      (.-y translate)]])))

(defn center
  [ref]
  (when-let [current (.-current ref)]
    (mat/div [(.-clientWidth current)
              (.-clientHeight current)] 2)))

(defn tree
  [ref]
  (let [tree-data @(rf/subscribe [::history.s/tree-data])
        zoom @(rf/subscribe [::history.s/zoom])
        dom-el (.-current ref)
        [x y] @(rf/subscribe [::history.s/translate])
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
       {:on-click #(rf/dispatch [::history.e/tree-view-updated 0.5 (center ref)])}
       "Center view"]
      [:button.button.flex-1
       {:on-click #(rf/dispatch [::dialog.e/confirmation
                                 {:title "This action cannot be undone."
                                  :description "Are you sure you wish to clear the document history?"
                                  :confirm-label "Clear history"
                                  :action [::history.e/clear]}])}
       "Clear history"]]
     [:div.flex-1 {:ref ref}
      [tree ref]]]))
