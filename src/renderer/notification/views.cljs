(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.events :as-alias events]
   [renderer.notification.events :as-alias notification.events]
   [renderer.notification.subs :as-alias notification.subs]
   [renderer.views :as views]))

(defn unavailable-feature
  [feature compatibility-url]
  [:div
   [:h2.font-bold.text-error (str feature " is unavailable")]
   [:div.mt-4
    "Your browser does not support this API."
    "You can check the "
    [:button.button-link
     {:on-click #(rf/dispatch [::events/open-remote-url compatibility-url])}
     "browser compatibility table."]]])

(defn generic-error
  [{:keys [title message]}]
  [:div
   [:h2.font-bold.text-error (or title "Error")]
   (when message [:div.mt-4 message])])

(defn exception
  [^js/Error error]
  (generic-error {:title (or (.-name error) "Error")
                  :message (or (.-message error) (str error))}))

(defn spec-failed
  [event error]
  [:div
   [:h2.font-bold.text-error "Validation error"]
   [:div.mt-4
    [:p "Event: " event]
    [:p error]]])

(defn main
  []
  (let [notifications @(rf/subscribe [::notification.subs/entities])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2.items-end
     (map-indexed
      (fn [index notification]
        [:div.relative.flex.bg-secondary.w-80.p-4.mb-2.rounded.shadow-md
         {:key index
          :class "border border-default"}
         (:content notification)
         [views/icon-button
          "times"
          {:aria-label "Close"
           :class "icon-button absolute top-3 right-3 small"
           :on-click #(rf/dispatch [::notification.events/remove-nth index])}]
         (when (> (:count notification) 1)
           [:div.absolute.bg-error.left-0.top-0.px-1.py-0.5.rounded
            {:class "-translate-x-1/2 -translate-y-1/2"}
            (:count notification)])])
      notifications)

     (when (second notifications)
       [:div.bg-primary
        [:button.button.overlay.px-2.rounded
         {:on-click #(rf/dispatch [::notification.events/clear-all])}
         "Clear all"]])]))
