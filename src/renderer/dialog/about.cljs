(ns renderer.dialog.about
  (:require
   [config]
   [platform]
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]))

(defn root
  []
  [:div.p-4
   [:div.flex.gap-3.items-start.pb-2
    [:img.w-24 {:src "/img/icon.svg"}]
    [:div
     [:h1.text-3xl.mb-2.font-light "Repath Studio"]
     [:div  [:strong "Version: "] config/version]
     [:div  [:strong "Browser: "] platform/user-agent]]]
   [:div.flex.justify-end
    [:button.button.px-2.bg-primary.rounded
     {:on-click #(rf/dispatch [:dialog/close])}
     "OK"]]
   [v/close]])
