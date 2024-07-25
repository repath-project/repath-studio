(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.notification.events :as-alias notification.e]))

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
     (map-indexed
      (fn [index notification]
        [:div.relative.flex.bg-secondary.w-80.p-4.mb-2.rounded.shadow-md.border.border-default
         {:key index}
         (:content notification)
         [comp/icon-button
          "times"
          {:aria-label "Close"
           :class "close-button small"
           :on-click #(rf/dispatch [::notification.e/remove index])}]
         (when-let [count (:count notification)]
           [:div.absolute.error.left-0.top-0.px-1.py-0.5.rounded
            {:class "-translate-x-1/2 -translate-y-1/2"}
            (inc count)])])
      notifications)

     (when (second notifications)
       [:div.bg-primary
        [:button.button.overlay.px-2.rounded
         {:on-click #(rf/dispatch [::notification.e/clear-all])}
         "Clear all"]])]))
