(ns renderer.notification.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn main
  []
  (let [notifications @(rf/subscribe [:notifications])]
    [:div.fixed.flex.flex-col.m-4.right-0.bottom-0.gap-2
     (map-indexed (fn [index notification]
                    [:div.toast
                     {:key index}
                     [:div.toast-description
                      (:content notification)]
                     [comp/icon-button
                      {:title "Dismiss"
                       :icon "times"
                       :action #(rf/dispatch-sync [:notification/remove index])}]])
                  notifications)]))