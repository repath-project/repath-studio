(ns renderer.home
  (:require
   [re-frame.core :as rf]))

(defn panel []
  [:div.flex.overflow-auto.flex-1.min-h-full.justify-center
   [:div.bg-primary.w-full.self-center.justify-center.p-12
    {:style {:max-width "1200px"}}

    [:h1.text-xl.mb-1 "repath.studio"]

    [:h4.mb-2 "Scalable Vector Graphics Manipulation"]

    [:h2.mb-1.mt-2 "Start"]

    [:div
     [:a
      {:on-click #(rf/dispatch [:document/new])}
      "New"]
     [:span.text-muted " (Ctrl+N)"]]

    [:div
     [:a
      {:on-click #(rf/dispatch [:document/open])}
      "Open"]
     [:span.text-muted " (Ctrl+O)"]]

    #_[:h2.text-xl "Recent"]

    [:h2.mb-1.mt-2 "Help"]

    [:div [:a {:href "https://repath.studio/" :target "_blank"} "Website"]]
    [:div [:a {:href "https://github.com/re-path/studio" :target "_blank"} "Source Code"]]
    [:div [:a {:href "https://repath.studio/roadmap/changelog/" :target "_blank"} "Changelog"]]
    [:div [:a {:href "https://github.com/re-path/studio/issues/new/choose" :target "_blank"} "Submit an issue"]]]])
