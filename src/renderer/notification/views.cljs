(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn main
  []
  (let [notifications @(rf/subscribe [:notifications])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2.items-end
     [:div
      (map-indexed
       (fn [index notification]
         [:div.toast
          {:key index
           :style {:right 0}}
          [:div.toast-description
           (:content notification)]
          [comp/icon-button
           "times"
           {:title "Dismiss"
            :style {:width "auto"
                    :height "fit-content"}
            :on-click #(rf/dispatch [:notification/remove index])}]
          (when-let [count (:count notification)]
            [:div.toast-count (inc count)])])
       notifications)]

     (when (second notifications)
       [:button.button.overlay.p-2
        {:on-click #(rf/dispatch [:notification/clear-all])}
        "Clear all"])]))
