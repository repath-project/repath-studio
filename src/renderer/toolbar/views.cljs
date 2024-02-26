(ns renderer.toolbar.views
  (:require
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.components :as comp]))

(defn button
  [{:keys [title icon disabled? action type]}]
  (if (= type :divider)
    [:span.h-divider]
    [:> Tooltip/Root
     [:> Tooltip/Trigger
      {:as-child true}
      [:span.shadow-4
       [comp/icon-button
        icon
        {:disabled disabled?
         :on-click #(rf/dispatch action)}]]]
     [:> Tooltip/Portal
      [:> Tooltip/Content
       {:class "tooltip-content"
        :side "left"}
       [:div.flex.gap-2.items-center
        title
        (when-let [shortcuts (comp/shortcuts action)]
          [:div.p-1.text-xs.bg-primary.rounded
           shortcuts])]
       [:> Tooltip/Arrow
        {:class "tooltip-arrow"}]]]]))
