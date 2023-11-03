(ns renderer.object
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [re-frame.registrar]
   ["@radix-ui/react-tooltip" :as Tooltip]))

(defn actions
  [selected-elements? multiple-selected?]
  [{:title "Bring to front"
    :icon "bring-front"
    :action [:elements/raise-to-top]
    :disabled? (not selected-elements?)}
   {:title "Send to back"
    :icon "send-back"
    :action [:elements/lower-to-bottom]
    :disabled? (not selected-elements?)}
   {:title "Bring forward"
    :icon "bring-forward"
    :action [:elements/raise]
    :disabled? (not selected-elements?)}
   {:title "Send backward"
    :icon "send-backward"
    :action [:elements/lower]
    :disabled? (not selected-elements?)}
   {:type :divider}
   {:title "Group"
    :icon "group"
    :disabled? (not selected-elements?)
    :action [:elements/group]}
   {:title "Ungroup"
    :icon "ungroup"
    :disabled? (not selected-elements?)
    :action [:elements/ungroup]}
   {:type :divider}
   {:title "Align left"
    :icon "objects-align-left"
    :disabled? (not selected-elements?)
    :action [:elements/align :left]}
   {:title "Align center horizontally"
    :disabled? (not selected-elements?)
    :icon "objects-align-center-horizontal"
    :action [:elements/align :center-horizontal]}
   {:title "Align rignt"
    :icon "objects-align-right"
    :disabled? (not selected-elements?)
    :action [:elements/align :right]}
   {:type :divider}
   {:title "Align top"
    :icon "objects-align-top"
    :disabled?  (not selected-elements?)
    :action [:elements/align :top]}
   {:title "Align center vertically"
    :icon "objects-align-center-vertical"
    :disabled?  (not selected-elements?)
    :action [:elements/align :center-vertical]}
   {:title "Align bottom"
    :icon "objects-align-bottom"
    :disabled?  (not selected-elements?)
    :action [:elements/align :bottom]}
   {:type :divider}
   {:title "Distribute spacing horizontally"
    :icon "distribute-spacing-horizontal"
    :disabled? true
    :action [:elements/raise]}
   {:title "Distribute spacing vertically"
    :icon "distribute-spacing-vertical"
    :disabled? true
    :action [:elements/lower]}
   #_{:type :divider}
   #_{:title "Rotate 90° clockwise"
      :icon "rotate-clockwise"
      :disabled? true
      :action [:elements/raise]}
   #_{:title "Rotate 90° counterclockwise"
      :icon "rotate-counterclockwise"
      :disabled? true
      :action [:elements/lower]}
   #_{:type :divider}
   #_{:title "Flip horizontally"
      :icon "flip-horizontal"
      :disabled? true
      :action [:elements/raise]}
   #_{:title "Flip vertically"
      :icon "flip-vertical"
      :disabled? true
      :action [:elements/lower]}
   {:type :divider}
   {:title "Unite"
    :icon "unite"
    :disabled? (not multiple-selected?) :action [:elements/bool-operation :unite]}
   {:title "Intersect"
    :icon "intersect"
    :disabled? (not multiple-selected?)
    :action [:elements/bool-operation :intersect]}
   {:title "Subtract"
    :icon "subtract"
    :disabled? (not multiple-selected?)
    :action [:elements/bool-operation :subtract]}
   {:title "Exclude"
    :icon "exclude"
    :disabled? (not multiple-selected?)
    :action [:elements/bool-operation :exclude]}
   {:title "divide"
    :icon "divide"
    :disabled? (not multiple-selected?)
    :action [:elements/bool-operation :divide]}])

(defn action-button
  [{:keys [title icon disabled? action type]}]
  (if (= type :divider)
    [:span.h-divider]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:asChild true}
      [:span.shadow-4
       [comp/icon-button
        icon
        {:disabled disabled?
         :on-click #(rf/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :side "left"}
       title
       [:> Tooltip/Arrow
        {:class "tooltip-arrow"}]]]]))

(defn toolbar
  []
  (let [selected-elements? @(rf/subscribe [:elements/selected?])
        multiple-selected? @(rf/subscribe [:elements/multiple-selected?])
        object-actions (actions selected-elements? multiple-selected?)]
    (into [:div.flex.flex-col.level-2.text-center.flex-0.ml-px.toolbar]
          (map action-button object-actions))))