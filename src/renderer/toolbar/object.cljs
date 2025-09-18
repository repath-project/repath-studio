(ns renderer.toolbar.object
  (:require
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.toolbar.views :as toolbar.views]
   [renderer.utils.i18n :refer [t]]))

(defn index-actions
  [disabled]
  [{:title (t [::bring-front "Bring to front"])
    :icon "bring-front"
    :action [::element.events/raise-to-top]
    :disabled disabled}
   {:title (t [::send-back "Send to back"])
    :icon "send-back"
    :action [::element.events/lower-to-bottom]
    :disabled disabled}
   {:title (t [::bring-forward "Bring forward"])
    :icon "bring-forward"
    :action [::element.events/raise]
    :disabled disabled}
   {:title (t [::send-backward "Send backward"])
    :icon "send-backward"
    :action [::element.events/lower]
    :disabled disabled}])

(defn group-actions
  [disabled]
  [{:title (t [::group "Group"])
    :icon "group"
    :disabled disabled
    :action [::element.events/group]}
   {:title (t [::ungroup "Ungroup"])
    :icon "ungroup"
    :disabled disabled
    :action [::element.events/ungroup]}])

(defn alignment-actions
  [disabled]
  [{:title (t [::align-left "Align left"])
    :icon "objects-align-left"
    :disabled disabled
    :action [::element.events/align :left]}
   {:title (t [::align-center-hor "Align center horizontally"])
    :disabled disabled
    :icon "objects-align-center-horizontal"
    :action [::element.events/align :center-horizontal]}
   {:title (t [::align-right "Align right"])
    :icon "objects-align-right"
    :disabled disabled
    :action [::element.events/align :right]}
   {:type :divider}
   {:title (t [::align-top "Align top"])
    :icon "objects-align-top"
    :disabled disabled
    :action [::element.events/align :top]}
   {:title (t [::align-center-ver "Align center vertically"])
    :icon "objects-align-center-vertical"
    :disabled disabled
    :action [::element.events/align :center-vertical]}
   {:title (t [::align-bottom "Align bottom"])
    :icon "objects-align-bottom"
    :disabled disabled
    :action [::element.events/align :bottom]}])

(defn boolean-actions
  [disabled]
  [{:title (t [::unite "Unite"])
    :icon "unite"
    :disabled disabled
    :action [::element.events/boolean-operation :unite]}
   {:title (t [::intersect "Intersect"])
    :icon "intersect"
    :disabled disabled
    :action [::element.events/boolean-operation :intersect]}
   {:title (t [::subtract "Subtract"])
    :icon "subtract"
    :disabled disabled
    :action [::element.events/boolean-operation :subtract]}
   {:title (t [::exclude "Exclude"])
    :icon "exclude"
    :disabled disabled
    :action [::element.events/boolean-operation :exclude]}
   {:title (t [::divide "Divide"])
    :icon "divide"
    :disabled disabled
    :action [::element.events/boolean-operation :divide]}])

#_(defn distribute-actions
    []
    [{:title "Distribute spacing horizontally"
      :icon "distribute-spacing-horizontal"
      :disabled true
      :action [::element.events/istribute-spacing :horizontal]}
     {:title "Distribute spacing vertically"
      :icon "distribute-spacing-vertical"
      :disabled true
      :action [::element.events/distribute-spacing :vertical]}])

#_(defn rotate-actions
    []
    [{:title "Rotate 90° clockwise"
      :icon "rotate-clockwise"
      :disabled true
      :action [::element.events/rotate -90]}
     {:title "Rotate 90° counterclockwise"
      :icon "rotate-counterclockwise"
      :disabled true
      :action [::element.events/rotate 90]}])

#_(defn flip-actions
    []
    [{:title "Flip horizontally"
      :icon "flip-horizontal"
      :disabled true
      :action [::element.events/flip :horizontal]}
     {:title "Flip vertically"
      :icon "flip-vertical"
      :disabled true
      :action [::element.events/flip :vertical]}])

(defn root
  []
  (let [some-selected? @(rf/subscribe [::element.subs/some-selected?])
        multiple-selected? @(rf/subscribe [::element.subs/multiple-selected?])
        every-top-level @(rf/subscribe [::element.subs/every-top-level])
        object-actions [(index-actions (not some-selected?))
                        (group-actions (not some-selected?))
                        (alignment-actions every-top-level)
                        (boolean-actions (not multiple-selected?))]]
    (->> object-actions
         (interpose [{:type :divider}])
         (flatten)
         (map toolbar.views/button)
         (into [:div.flex.flex-col.text-center.flex-0.toolbar]))))
