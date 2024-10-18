(ns renderer.toolbar.object
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.e]
   [renderer.element.subs :as-alias element.s]
   [renderer.toolbar.views :as v]))

(defn index-actions
  [disabled]
  [{:title "Bring to front"
    :icon "bring-front"
    :action [::element.e/raise-to-top]
    :disabled disabled}
   {:title "Send to back"
    :icon "send-back"
    :action [::element.e/lower-to-bottom]
    :disabled disabled}
   {:title "Bring forward"
    :icon "bring-forward"
    :action [::element.e/raise]
    :disabled disabled}
   {:title "Send backward"
    :icon "send-backward"
    :action [::element.e/lower]
    :disabled disabled}])

(defn group-actions
  [disabled]
  [{:title "Group"
    :icon "group"
    :disabled disabled
    :action [::element.e/group]}
   {:title "Ungroup"
    :icon "ungroup"
    :disabled disabled
    :action [::element.e/ungroup]}])

(defn alignment-actions
  [disabled]
  [{:title "Align left"
    :icon "objects-align-left"
    :disabled disabled
    :action [::element.e/align :left]}
   {:title "Align center horizontally"
    :disabled disabled
    :icon "objects-align-center-horizontal"
    :action [::element.e/align :center-horizontal]}
   {:title "Align rignt"
    :icon "objects-align-right"
    :disabled disabled
    :action [::element.e/align :right]}
   {:type :divider}
   {:title "Align top"
    :icon "objects-align-top"
    :disabled  disabled
    :action [::element.e/align :top]}
   {:title "Align center vertically"
    :icon "objects-align-center-vertical"
    :disabled  disabled
    :action [::element.e/align :center-vertical]}
   {:title "Align bottom"
    :icon "objects-align-bottom"
    :disabled  disabled
    :action [::element.e/align :bottom]}])

(defn boolean-actions
  [disabled]
  [{:title "Unite"
    :icon "unite"
    :disabled disabled
    :action [::element.e/boolean-operation :unite]}
   {:title "Intersect"
    :icon "intersect"
    :disabled disabled
    :action [::element.e/boolean-operation :intersect]}
   {:title "Subtract"
    :icon "subtract"
    :disabled disabled
    :action [::element.e/boolean-operation :subtract]}
   {:title "Exclude"
    :icon "exclude"
    :disabled disabled
    :action [::element.e/boolean-operation :exclude]}
   {:title "Divide"
    :icon "divide"
    :disabled disabled
    :action [::element.e/boolean-operation :divide]}])

#_(defn distribute-actions
    []
    [{:title "Distribute spacing horizontally"
      :icon "distribute-spacing-horizontal"
      :disabled true
      :action [::element.e/istribute-spacing :horizontal]}
     {:title "Distribute spacing vertically"
      :icon "distribute-spacing-vertical"
      :disabled true
      :action [::element.e/distribute-spacing :vertical]}])

#_(defn rotate-actions
    []
    [{:title "Rotate 90° clockwise"
      :icon "rotate-clockwise"
      :disabled true
      :action [::element.e/rotate -90]}
     {:title "Rotate 90° counterclockwise"
      :icon "rotate-counterclockwise"
      :disabled true
      :action [::element.e/rotate 90]}])

#_(defn flip-actions
    []
    [{:title "Flip horizontally"
      :icon "flip-horizontal"
      :disabled true
      :action [::element.e/flip :horizontal]}
     {:title "Flip vertically"
      :icon "flip-vertical"
      :disabled true
      :action [::element.e/flip :vertical]}])

(defn root
  []
  (let [some-selected @(rf/subscribe [::element.s/some-selected])
        multiple-selected @(rf/subscribe [::element.s/multiple-selected])
        every-top-level @(rf/subscribe [::element.s/every-top-level])
        object-actions [(index-actions (not some-selected))
                        (group-actions (not some-selected))
                        (alignment-actions every-top-level)
                        (boolean-actions (not multiple-selected))]]
    (->> object-actions
         (interpose [{:type :divider}])
         (flatten)
         (map v/button)
         (into [:div.flex.flex-col.text-center.flex-0.toolbar]))))
