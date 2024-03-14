(ns renderer.dialog.about
  (:require
   [config]
   [renderer.dialog.views :as v]))

(defn root
  []
  [:div.p-4.flex.gap-3
   [:img.w-24 {:src "/img/icon.svg"}]
   [:div
    [:h1.text-3xl.mb-2 "Repath Studio"]
    [:div (str "Version: " config/version)]
    [:div (str "Platform: " js/window.api.platform)]]
   [v/close]])
