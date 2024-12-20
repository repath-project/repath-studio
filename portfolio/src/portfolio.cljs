(ns portfolio
  (:require [components.button-scenes]
            [components.icon-scenes]
            [components.shortcuts-scenes]
            [components.slider-scenes]
            [components.switch-scenes]
            [portfolio.ui :as ui]
            [sections.home-scenes]))

(ui/start!
 {:config
  {:css-paths ["/main.css"]
   :background/options [{:id :light-mode
                         :title "Light mode"
                         :value {:background/background-color "#fff"
                                 :background/document-data {:theme "light"}}}
                        {:id :dark-mode
                         :title "Dark mode"
                         :value {:background/background-color "#111"
                                 :background/document-data {:theme "dark"}}}]
   :background/default-option-id :dark-mode
   :viewport/defaults {:viewport/padding [0]
                       :viewport/width "100%"
                       :viewport/height "500px"}
   :canvas/gallery-defaults {:viewport/padding [0]
                             :viewport/width "100%"
                             :viewport/height 41}}})

(defn ^:export init! [])
