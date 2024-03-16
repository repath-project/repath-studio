(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn unavailable-feature
  [feature compatibility-url]
  [:div
   [:h2.pb-4.font-bold feature " is unavailable."]
   [:div
    "Your browser does not support this API."
    "You can check the "
    [:a {:href compatibility-url}
     "browser compatibility table."]]])

(defn main
  []
  (let [notifications @(rf/subscribe [:notifications])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2.items-end
     [:div
      (map-indexed
       (fn [index notification]
         [:div.toast
          {:key index}
          [:div.toast-description
           (:content notification)]
          [comp/icon-button
           "times"
           {:aria-label "Close"
            :class "close-button small"
            :on-click #(rf/dispatch [:notification/remove index])}]
          (when-let [count (:count notification)]
            [:div.toast-count (inc count)])])
       notifications)]

     (when (second notifications)
       [:button.button.overlay.px-2
        {:on-click #(rf/dispatch [:notification/clear-all])}
        "Clear all"])]))
