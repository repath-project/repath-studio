(ns renderer.document.db
  (:require
   [renderer.element.db :as element-db]))

(def document
  [:map
   [:hovered-keys [:set uuid?]]
   [:ignored-keys [:set uuid?]]
   [:active-page uuid?]
   [:fill string?]
   [:stroke string?]
   [:zoom double?]
   [:rotate double?]
   [:filter string?]
   [:pan [:tuple double? double?]]
   [:elements [:map-of :uuid element-db/element]]])

(def default-document
  {:hovered-keys #{}
   :ignored-keys #{}
   :active-page :default-page
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
