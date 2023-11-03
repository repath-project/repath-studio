(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn main
  []
  (let [notifications @(rf/subscribe [:notifications])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2.items-end

     (when (second notifications)
       [:button.button.level-3.p-2
        {:on-click #(rf/dispatch [:notification/clear-all])}
        "Clear all"])

     (map-indexed
      (fn [index notification]
        [:div.toast
         {:key index}
         [:div.toast-description
          (:content notification)]
         [comp/icon-button
          "times"
          {:title "Dismiss"
           :on-click #(rf/dispatch [:notification/remove index])}]])
      notifications)]))