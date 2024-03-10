(ns renderer.home
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn panel []
  (let [recent @(rf/subscribe [:document/recent])]
    [:div.flex.overflow-auto.flex-1.min-h-full.justify-center
     [:div.bg-primary.w-full.self-center.justify-between.p-12.flex
      {:style {:max-width "1200px"}}

      [:div
       [:h1.text-4xl.mb-1.font-light "Repath Studio"]

       [:p.text-xl.text-muted.font-bold "Scalable Vector Graphics Manipulation"]

       [:h2.mb-3.mt-8.text-2xl "Start"]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [:document/new])} "New"]
        [comp/shortcuts [:document/new]]]

       [:div
        [:button.text-lg.text-accent.mr-2
         {:on-click #(rf/dispatch [:document/open])} "Open"]
        [comp/shortcuts [:document/open]]]

       [:h2.mb-3.mt-8.text-2xl
        {:class (when-not (seq recent) "text-muted")}
        "Recent"]

       (for [file-path (take 2 recent)]
         ^{:key file-path}
         [:button.text-lg.text-accent
          {:on-click #(rf/dispatch [:document/open file-path])}
          file-path])

       [:h2.mb-3.mt-8.text-2xl "Help"]

       [:a.text-lg.block
        {:href "https://repath.studio/"
         :target "_blank"}
        "Website"]
       [:a.text-lg.block
        {:href "https://github.com/re-path/studio"
         :target "_blank"}
        "Source Code"]
       [:a.text-lg.block
        {:href "https://repath.studio/roadmap/changelog/"
         :target "_blank"}
        "Changelog"]]

      [:div
       [:img {:src "/img/icon.svg"}]]]]))
