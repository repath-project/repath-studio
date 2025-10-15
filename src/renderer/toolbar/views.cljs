(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.views :as views]))

(defn button
  [{:keys [title icon disabled action]
    :as attrs}]
  (if (= (:type attrs) :divider)
    [:span.h-divider]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span.shadow-4
       [views/icon-button icon {:disabled disabled
                                :aria-label title
                                :on-click #(rf/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :side "left"
        :sideOffset 5
        :on-escape-key-down #(.stopPropagation %)}
       [:div.flex.gap-2.items-center
        title
        [views/shortcuts action]]]]]))
