(ns renderer.dialog.confirmation
  (:require
   [config]
   [platform]
   [re-frame.core :as rf]
   [renderer.dialog.views :as v]))

(defn root
  [{:keys [title description action]}]
  [:div.p-4
   [:h1.text-3xl.mb-2.font-light title]
   [:div description]
   [:div.flex.justify-end
    [:button.button.px-2.bg-primary.rounded
     {:on-click #(rf/dispatch [:dialog/close])}
     "No"]
    [:button.button.px-2.bg-primary.rounded
     {:on-click #(do (rf/dispatch [:dialog/close])
                     (rf/dispatch action))}
     "Yes"]]
   [v/close]])
