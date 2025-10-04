(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.events :as-alias events]
   [renderer.notification.events :as-alias notification.events]
   [renderer.notification.subs :as-alias notification.subs]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defn unavailable-feature
  [feature compatibility-url]
  [:div
   [:h2.font-bold.text-error.pr-5
    (str feature " is unavailable")]
   [:div.mt-4
    "Your browser does not support this API."
    "You can check the "
    [:button.button-link
     {:on-click #(rf/dispatch [::events/open-remote-url compatibility-url])}
     "browser compatibility table."]]])

(defn generic-error
  [{:keys [title message]}]
  [:div
   [:h2.font-bold.text-error.pr-5
    (or title (t [::error "Error"]))]
   (when message
     [:div.mt-4 message])])

(defn exception
  [^js/Error error]
  (generic-error {:title (or (.-name error)
                             (t [::error "Error"]))
                  :message (or (.-message error)
                               (str error))}))

(defn spec-failed
  [event error]
  [:div
   [:h2.font-bold.text-error.pr-5
    (t [::validation-error "Validation error"])]
   [:div.mt-4
    [:p (t [::event "Event: "]) event]
    [:p error]]])

(defn notification-popup
  [index notification]
  [:div.relative.flex.bg-primary.w-80.p-4.mb-2.rounded.shadow-md
   {:class "border border-default"}
   (:content notification)
   [views/icon-button "times"
    {:aria-label (t [::close "Close"])
     :class "icon-button absolute small right-3 rtl:right-auto rtl:left-3"
     :on-click #(rf/dispatch [::notification.events/remove-nth index])}]
   (when (> (:count notification) 1)
     [:div.absolute.bg-error.left-0.top-0.px-1.py-0.5.rounded
      {:class "-translate-x-1/2 -translate-y-1/2"}
      (:count notification)])])

(defn root
  []
  (let [notifications @(rf/subscribe [::notification.subs/entities])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2.items-end
     {:class "rtl:right-auto rtl:left-0"}
     (into [:<>] (map-indexed notification-popup notifications))
     (when (second notifications)
       [:div.bg-primary.border.border-default.rounded.shadow-md
        [:button.button.px-2.rounded
         {:on-click #(rf/dispatch [::notification.events/clear-all])}
         (t [::clear-all "Clear all"])]])]))
