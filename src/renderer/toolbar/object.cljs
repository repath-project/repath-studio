(ns renderer.toolbar.object
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.element.events :as-alias element.events]
   [renderer.element.subs :as-alias element.subs]
   [renderer.element.views :as element.views]
   [renderer.toolbar.views :as toolbar.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn index-actions
  [disabled]
  [{:label (t [::bring-front "Bring to front"])
    :icon "bring-front"
    :action [::element.events/raise-to-top]
    :disabled disabled}
   {:label (t [::send-back "Send to back"])
    :icon "send-back"
    :action [::element.events/lower-to-bottom]
    :disabled disabled}
   {:label (t [::bring-forward "Bring forward"])
    :icon "bring-forward"
    :action [::element.events/raise]
    :disabled disabled}
   {:label (t [::send-backward "Send backward"])
    :icon "send-backward"
    :action [::element.events/lower]
    :disabled disabled}])

#_(defn group-actions
    [disabled]
    [{:label (t [::group "Group"])
      :icon "group"
      :disabled disabled
      :action [::element.events/group]}
     {:label (t [::ungroup "Ungroup"])
      :icon "ungroup"
      :disabled disabled
      :action [::element.events/ungroup]}])

(defn alignment-actions
  [disabled]
  [{:label (t [::align-left "Align left"])
    :icon "objects-align-left"
    :disabled disabled
    :action [::element.events/align :left]}
   {:label (t [::align-center-hor "Align center horizontally"])
    :disabled disabled
    :icon "objects-align-center-horizontal"
    :action [::element.events/align :center-horizontal]}
   {:label (t [::align-right "Align right"])
    :icon "objects-align-right"
    :disabled disabled
    :action [::element.events/align :right]}
   {:type :divider}
   {:label (t [::align-top "Align top"])
    :icon "objects-align-top"
    :disabled disabled
    :action [::element.events/align :top]}
   {:label (t [::align-center-ver "Align center vertically"])
    :icon "objects-align-center-vertical"
    :disabled disabled
    :action [::element.events/align :center-vertical]}
   {:label (t [::align-bottom "Align bottom"])
    :icon "objects-align-bottom"
    :disabled disabled
    :action [::element.events/align :bottom]}])

(defn boolean-actions
  [disabled]
  [{:label (t [::unite "Unite"])
    :icon "unite"
    :disabled disabled
    :action [::element.events/boolean-operation :unite]}
   {:label (t [::intersect "Intersect"])
    :icon "intersect"
    :disabled disabled
    :action [::element.events/boolean-operation :intersect]}
   {:label (t [::subtract "Subtract"])
    :icon "subtract"
    :disabled disabled
    :action [::element.events/boolean-operation :subtract]}
   {:label (t [::exclude "Exclude"])
    :icon "exclude"
    :disabled disabled
    :action [::element.events/boolean-operation :exclude]}
   {:label (t [::divide "Divide"])
    :icon "divide"
    :disabled disabled
    :action [::element.events/boolean-operation :divide]}])

#_(defn distribute-actions
    []
    [{:label "Distribute spacing horizontally"
      :icon "distribute-spacing-horizontal"
      :disabled true
      :action [::element.events/istribute-spacing :horizontal]}
     {:label "Distribute spacing vertically"
      :icon "distribute-spacing-vertical"
      :disabled true
      :action [::element.events/distribute-spacing :vertical]}])

#_(defn rotate-actions
    []
    [{:label "Rotate 90° clockwise"
      :icon "rotate-clockwise"
      :disabled true
      :action [::element.events/rotate -90]}
     {:label "Rotate 90° counterclockwise"
      :icon "rotate-counterclockwise"
      :disabled true
      :action [::element.events/rotate 90]}])

#_(defn flip-actions
    []
    [{:label "Flip horizontally"
      :icon "flip-horizontal"
      :disabled true
      :action [::element.events/flip :horizontal]}
     {:label "Flip vertically"
      :icon "flip-vertical"
      :disabled true
      :action [::element.events/flip :vertical]}])

(defn more-button []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:as-child true}
    [:button.button.flex.items-center.justify-center.px-2.font-mono.rounded
     {:aria-label "More object actions"}
     [views/icon "ellipsis-h"]]]
   [:> DropdownMenu/Portal
    (->> (element.views/context-menu)
         (map views/dropdown-menu-item)
         (into [:> DropdownMenu/Content
                {:side "left"
                 :align "end"
                 :class "menu-content rounded-sm"
                 :on-key-down #(.stopPropagation %)
                 :on-escape-key-down #(.stopPropagation %)}
                [:> DropdownMenu/Arrow {:class "fill-primary"}]]))]])

(defn object-buttons
  []
  (let [some-selected? @(rf/subscribe [::element.subs/some-selected?])
        multiple-selected? @(rf/subscribe [::element.subs/multiple-selected?])
        every-top-level? @(rf/subscribe [::element.subs/every-top-level?])
        md? @(rf/subscribe [::window.subs/md?])
        object-actions [(index-actions (not some-selected?))
                        (when md? (alignment-actions every-top-level?))
                        (boolean-actions (not multiple-selected?))]]
    (->> (keep identity object-actions)
         (interpose [{:type :divider}])
         (flatten)
         (map toolbar.views/button))))

(defn root
  []
  (let [some-selected? @(rf/subscribe [::element.subs/some-selected?])
        md? @(rf/subscribe [::window.subs/md?])]
    [views/toolbar {:class "flex-col"}
     (into [:<>] (object-buttons))
     (when-not md?
       [:<>
        [:span.h-divider]
        [more-button (not some-selected?)]])]))
