(ns renderer.history.views
  (:require
   ["@radix-ui/react-select" :as Select]
   ["react-d3-tree" :refer [Tree]]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]))

(defn select-options
  [history-list]
  (for [{:keys [id explanation]} history-list]
    ^{:key id}
    [:> Select/Item
     {:value (name id)
      :class "menu-item select-item"}
     [:> Select/ItemText explanation]]))

(defn select
  [label options disabled?]
  [:> Select/Root {:onValueChange #(rf/dispatch [:history/move (keyword %)])
                   :disabled disabled?}
   [:> Select/Trigger
    {:class "select-trigger"
     :aria-label label
     :style {:background "transparent"
             :width "16px"
             :margin 0}}
    [:> Select/Value ""]
    [:> Select/Icon
     [comp/icon "chevron-down" {:class "small"}]]]
   [:> Select/Portal
    [:> Select/Content
     {:side "top"
      :sideOffset 5
      :position "popper"
      :class "menu-content rounded select-content"}
     [:> Select/ScrollUpButton {:class "select-scroll-button"}
      [comp/icon "chevron-up"]]
     [:> Select/Viewport {:class "select-viewport"}
      [:> Select/Group
       (select-options options)]]
     [:> Select/ScrollDownButton
      {:class "select-scroll-button"}
      [comp/icon "chevron-down"]]]]])

(defn node
  [node]
  (let [datum (.-nodeDatum node)]
    (ra/as-element
     [:circle
      {:on-click #(rf/dispatch [:history/move (keyword (.-id datum))])
       :cx "0"
       :cy "0"
       :r "12"
       :fill (if (.-restored datum)
               "black"
               (if (.-active datum)
                 "var(--accent)"
                 (.-color datum)))}
      [:title (.-name datum)]])))

(defn tree
  []
  (let [tree-data @(rf/subscribe [:history/tree-data])]
    [:> Tree {:data tree-data
              :collapsible false
              :orientation "vertical"
              :rootNodeClassName "root-node"
              :depthFactor -100
              :zoom 0.5
              :separation #js {:nonSiblings 1 :siblings 1}
              :translate #js {:x "100" :y "100"}
              :renderCustomNodeElement node}]))

(defn root
  []
  [:div.flex.flex-col.h-full
   [:div.flex.p-1
    [:button.button.flex-auto
     {:on-click #(rf/dispatch [:history/undo])
      :disabled (not @(rf/subscribe [:history/undos?]))}
     "Undo"]
    [:button.button.flex-auto
     {:on-click #(rf/dispatch [:history/redo])
      :disabled (not @(rf/subscribe [:history/redos?]))}
     "Redo"]
    [:button.button.flex-auto
     {:on-click #(rf/dispatch [:history/clear])}
     "Clear history"]]
   [:div.flex-1
    [tree]]])
