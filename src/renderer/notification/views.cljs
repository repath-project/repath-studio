(ns renderer.notification.views
  (:require
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [renderer.notification.events :as-alias notification.e]
   [renderer.notification.subs :as-alias notification.s]
   [renderer.ui :as ui]
   [renderer.window.events :as-alias window.e]))

(mx/defn unavailable-feature
  [feature :- string?, compatibility-url :- string?]
  [:div
   [:h2.pb-4.font-bold feature " is unavailable."]
   [:div
    "Your browser does not support this API."
    "You can check the "
    [:button.button-link
     {:on-click #(rf/dispatch [::window.e/open-remote-url compatibility-url])}
     "browser compatibility table."]]])

(mx/defn spec-failed
  [event :- string?, error :- string?]
  [:div
   [:h2.mb-4.font-bold "Validation error"]
   [:p "Event: " event]
   [:p.text-error error]])

(defn main
  []
  (let [notifications @(rf/subscribe [::notification.s/notifications])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2.items-end
     (map-indexed
      (fn [index notification]
        [:div.relative.flex.bg-secondary.w-80.p-4.mb-2.rounded.shadow-md.border.border-default
         {:key index}
         (:content notification)
         [ui/icon-button
          "times"
          {:aria-label "Close"
           :class "close-button"
           :on-click #(rf/dispatch [::notification.e/remove index])}]
         (when-let [n (:count notification)]
           [:div.absolute.bg-error.left-0.top-0.px-1.py-0.5.rounded
            {:class "-translate-x-1/2 -translate-y-1/2"}
            (inc n)])])
      notifications)

     (when (second notifications)
       [:div.bg-primary
        [:button.button.overlay.px-2.rounded
         {:on-click #(rf/dispatch [::notification.e/clear-all])}
         "Clear all"]])]))
