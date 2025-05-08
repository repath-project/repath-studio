(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.ui :as ui]))

(defn button
  [{:keys [title icon disabled action] :as attrs}]
  (if (= (:type attrs) :divider)
    [:span.h-divider]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span.shadow-4
       [ui/icon-button icon {:disabled disabled
                             :aria-label title
                             :on-click #(rf/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :side "left"
        :sideOffset 5}
       [:div.flex.gap-2.items-center
        title]]]]))
