(ns renderer.toolbar.object
  (:require
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.toolbar.views :as v]))

(defn actions
  [selected-elements? multiple-selected?]
  [{:title "Bring to front"
    :icon "bring-front"
    :action [:element/raise-to-top]
    :disabled? (not selected-elements?)}
   {:title "Send to back"
    :icon "send-back"
    :action [:element/lower-to-bottom]
    :disabled? (not selected-elements?)}
   {:title "Bring forward"
    :icon "bring-forward"
    :action [:element/raise]
    :disabled? (not selected-elements?)}
   {:title "Send backward"
    :icon "send-backward"
    :action [:element/lower]
    :disabled? (not selected-elements?)}
   {:type :divider}
   {:title "Group"
    :icon "group"
    :disabled? (not selected-elements?)
    :action [:element/group]}
   {:title "Ungroup"
    :icon "ungroup"
    :disabled? (not selected-elements?)
    :action [:element/ungroup]}
   {:type :divider}
   {:title "Align left"
    :icon "objects-align-left"
    :disabled? (not selected-elements?)
    :action [:element/align :left]}
   {:title "Align center horizontally"
    :disabled? (not selected-elements?)
    :icon "objects-align-center-horizontal"
    :action [:element/align :center-horizontal]}
   {:title "Align rignt"
    :icon "objects-align-right"
    :disabled? (not selected-elements?)
    :action [:element/align :right]}
   {:type :divider}
   {:title "Align top"
    :icon "objects-align-top"
    :disabled?  (not selected-elements?)
    :action [:element/align :top]}
   {:title "Align center vertically"
    :icon "objects-align-center-vertical"
    :disabled?  (not selected-elements?)
    :action [:element/align :center-vertical]}
   {:title "Align bottom"
    :icon "objects-align-bottom"
    :disabled?  (not selected-elements?)
    :action [:element/align :bottom]}
   {:type :divider}
   {:title "Distribute spacing horizontally"
    :icon "distribute-spacing-horizontal"
    :disabled? true
    :action [:element/raise]}
   {:title "Distribute spacing vertically"
    :icon "distribute-spacing-vertical"
    :disabled? true
    :action [:element/lower]}
   #_{:type :divider}
   #_{:title "Rotate 90° clockwise"
      :icon "rotate-clockwise"
      :disabled? true
      :action [:element/raise]}
   #_{:title "Rotate 90° counterclockwise"
      :icon "rotate-counterclockwise"
      :disabled? true
      :action [:element/lower]}
   #_{:type :divider}
   #_{:title "Flip horizontally"
      :icon "flip-horizontal"
      :disabled? true
      :action [:element/raise]}
   #_{:title "Flip vertically"
      :icon "flip-vertical"
      :disabled? true
      :action [:element/lower]}
   {:type :divider}
   {:title "Unite"
    :icon "unite"
    :disabled? (not multiple-selected?) :action [:element/bool-operation :unite]}
   {:title "Intersect"
    :icon "intersect"
    :disabled? (not multiple-selected?)
    :action [:element/bool-operation :intersect]}
   {:title "Subtract"
    :icon "subtract"
    :disabled? (not multiple-selected?)
    :action [:element/bool-operation :subtract]}
   {:title "Exclude"
    :icon "exclude"
    :disabled? (not multiple-selected?)
    :action [:element/bool-operation :exclude]}
   {:title "Divide"
    :icon "divide"
    :disabled? (not multiple-selected?)
    :action [:element/bool-operation :divide]}])

(defn root
  []
  (let [selected-elements? @(rf/subscribe [:element/selected?])
        multiple-selected? @(rf/subscribe [:element/multiple-selected?])
        object-actions (actions selected-elements? multiple-selected?)]
    (into [:div.flex.flex-col.level-2.text-center.flex-0.ml-px.toolbar]
          (map v/button object-actions))))
