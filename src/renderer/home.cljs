(ns renderer.home
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.dialog.events :as-alias dialog.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.document.events :as-alias document.e]))

(defn panel []
  (let [recent @(rf/subscribe [::document.s/recent])]
    [:div.flex.overflow-auto.flex-1.min-h-full.justify-center
     [:div.bg-primary.w-full.self-center.justify-between.p-12.flex.max-w-screen-xl
      [:div
       [:h1.text-4xl.mb-1.font-light
        "Repath Studio"]

       [:p.text-xl.text-muted.font-bold
        "Scalable Vector Graphics Manipulation"]

       [:h2.mb-3.mt-8.text-2xl "Start"]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [::document.e/new])} "New"]
        [comp/shortcuts [::document.e/new]]]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [::document.e/open])}
         "Open"]
        [comp/shortcuts [::document.e/open]]]

       [:h2.mb-3.mt-8.text-2xl
        {:class (when-not (seq recent) "text-muted")}
        "Recent"]

       (for [file-path (take 2 recent)]
         ^{:key file-path}
         [:button.text-lg.text-accent.block
          {:on-click #(rf/dispatch [::document.e/open file-path])}
          file-path])

       [:h2.mb-3.mt-8.text-2xl "Help"]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [::dialog.e/cmdk])}
         "Command panel"]
        [comp/shortcuts [::dialog.e/cmdk]]]
       [:a.text-lg.block
        {:href "https://repath.studio/"
         :target "_blank"}
        "Website"]
       [:a.text-lg.block
        {:href "https://github.com/repath-project/repath-studio"
         :target "_blank"}
        "Source Code"]
       [:a.text-lg.block
        {:href "https://repath.studio/roadmap/changelog/"
         :target "_blank"}
        "Changelog"]]

      [:div
       [:img {:src "img/icon.svg"}]]]]))
