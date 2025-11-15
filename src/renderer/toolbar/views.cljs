(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.i18n.views :as i18n.views]
   [renderer.views :as views]))

(defn button
  [{:keys [label icon disabled action]
    :as attrs}]
  (if (= (:type attrs) :divider)
    [:span.h-divider]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span.shadow-4
       [views/icon-button icon {:disabled disabled
                                :aria-label (i18n.views/t label)
                                :on-click #(rf/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :side "left"
        :sideOffset 5
        :on-escape-key-down #(.stopPropagation %)}
       [:div.flex.gap-2.items-center
        (i18n.views/t label)
        [views/shortcuts action]]]]]))
