(ns renderer.document.db)

(def default-document
  {:hovered-keys #{}
   :ignored-keys #{}
   :active-page :default-page
   :rulers-locked? false
   :grid? false
   :rulers? true
   :fill "#ffffff"
   :stroke "#000000"
   :zoom 1
   :rotate 0
   :filter "No a11y filter"
   :pan [0.0]
   :elements {:canvas {:key :canvas
                       :visible? true
                       :tag :canvas
                       :type :element
                       :attrs {:fill "#eeeeee"}
                       :children [:default-page]}
              :default-page {:key :default-page
                             :name "Page"
                             :visible? true
                             :type :element
                             :tag :page
                             :parent :canvas
                             :attrs {:width 800
                                     :height 600
                                     :x 0
                                     :y 0
                                     :fill "#ffffff"}
                             :children []}}})
