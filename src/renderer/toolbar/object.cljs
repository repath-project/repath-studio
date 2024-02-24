(ns renderer.toolbar.object
  (:require
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.toolbar.views :as v]))

(defn index-actions
  [disabled?]
  [{:title "Bring to front"
    :icon "bring-front"
    :action [:element/raise-to-top]
    :disabled? disabled?}
   {:title "Send to back"
    :icon "send-back"
    :action [:element/lower-to-bottom]
    :disabled? disabled?}
   {:title "Bring forward"
    :icon "bring-forward"
    :action [:element/raise]
    :disabled? disabled?}
   {:title "Send backward"
    :icon "send-backward"
    :action [:element/lower]
    :disabled? disabled?}])

(defn group-actions
  [disabled?]
  [{:title "Group"
    :icon "group"
    :disabled? disabled?
    :action [:element/group]}
   {:title "Ungroup"
    :icon "ungroup"
    :disabled? disabled?
    :action [:element/ungroup]}])

(defn alignment-actions
  [disabled?]
  [{:title "Align left"
    :icon "objects-align-left"
    :disabled? disabled?
    :action [:element/align :left]}
   {:title "Align center horizontally"
    :disabled? disabled?
    :icon "objects-align-center-horizontal"
    :action [:element/align :center-horizontal]}
   {:title "Align rignt"
    :icon "objects-align-right"
    :disabled? disabled?
    :action [:element/align :right]}
   {:type :divider}
   {:title "Align top"
    :icon "objects-align-top"
    :disabled?  disabled?
    :action [:element/align :top]}
   {:title "Align center vertically"
    :icon "objects-align-center-vertical"
    :disabled?  disabled?
    :action [:element/align :center-vertical]}
   {:title "Align bottom"
    :icon "objects-align-bottom"
    :disabled?  disabled?
    :action [:element/align :bottom]}])

(defn boolean-actions
  [disabled?]
  [{:title "Unite"
    :icon "unite"
    :disabled? disabled?
    :action [:element/bool-operation :unite]}
   {:title "Intersect"
    :icon "intersect"
    :disabled? disabled?
    :action [:element/bool-operation :intersect]}
   {:title "Subtract"
    :icon "subtract"
    :disabled? disabled?
    :action [:element/bool-operation :subtract]}
   {:title "Exclude"
    :icon "exclude"
    :disabled? disabled?
    :action [:element/bool-operation :exclude]}
   {:title "Divide"
    :icon "divide"
    :disabled? disabled?
    :action [:element/bool-operation :divide]}])

(defn distribute-actions
  []
  [{:title "Distribute spacing horizontally"
    :icon "distribute-spacing-horizontal"
    :disabled? true
    :action [:element/raise]}
   {:title "Distribute spacing vertically"
    :icon "distribute-spacing-vertical"
    :disabled? true
    :action [:element/lower]}])

(defn rotate-actions
  []
  [{:title "Rotate 90° clockwise"
    :icon "rotate-clockwise"
    :disabled? true
    :action [:element/raise]}
   {:title "Rotate 90° counterclockwise"
    :icon "rotate-counterclockwise"
    :disabled? true
    :action [:element/lower]}])

(defn flip-actions
  []
  [{:title "Flip horizontally"
    :icon "flip-horizontal"
    :disabled? true
    :action [:element/raise]}
   {:title "Flip vertically"
    :icon "flip-vertical"
    :disabled? true
    :action [:element/lower]}])

(defn root
  []
  (let [selected-elements? @(rf/subscribe [:element/selected?])
        multiple-selected? @(rf/subscribe [:element/multiple-selected?])
        top-level? @(rf/subscribe [:element/top-level?])
        object-actions [(index-actions (not selected-elements?))
                        (group-actions (not selected-elements?))
                        (alignment-actions top-level?)
                        (boolean-actions (not multiple-selected?))]]
    (->> object-actions
         (interpose [{:type :divider}])
         flatten
         (map v/button)
         (into [:div.flex.flex-col.level-1.text-center.flex-0.ml-px.toolbar]))))
