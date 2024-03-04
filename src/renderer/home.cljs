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
        [:a.text-lg {:on-click #(rf/dispatch [:document/new])} "New"]
        (when-let [shortcuts (comp/shortcuts [:document/new])]
          [:span.shortcut.ml-2 shortcuts])]

       [:div
        [:a.text-lg {:on-click #(rf/dispatch [:document/open])} "Open"]
        (when-let [shortcuts (comp/shortcuts [:document/open])]
          [:span.shortcut.ml-2 shortcuts])]

       [:h2.mb-3.mt-8.text-2xl
        {:class (when-not (seq recent) "text-muted")}
        "Recent"]

       (for [file-path (take 2 recent)]
         [:a.text-lg.block {:on-click #(rf/dispatch [:document/open file-path])} file-path])

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
