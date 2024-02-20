(ns renderer.document.db
  (:require
   [renderer.element.db :as element.db]))

(def document
  [:map
   [:hovered-keys [:set uuid?]]
   [:collapsed-keys [:set uuid?]]
   [:ignored-keys [:set uuid?]]
   [:fill string?]
   [:stroke string?]
   [:zoom double?]
   [:rotate double?]
   [:filter string?]
   [:pan [:tuple double? double?]]
   [:elements [:map-of :uuid element.db/element]]])

(def default-document
  {:hovered-keys #{}
   :collapsed-keys #{}
   :ignored-keys #{}
   :fill "white"
   :stroke "black"
   :zoom 1
   :rotate 0
   :filter "No a11y filter"
   :pan [0 0]
   :elements {:canvas {:key :canvas
                       :visible? true
                       :tag :canvas
                       :type :element
                       :attrs {:fill "#eeeeee"}
                       :children [:default-page]}
              :default-page {:key :default-page
                             :visible? true
                             :selected? false
                             :type :element
                             :tag :svg
                             :parent :canvas
                             :attrs {:width 800
                                     :height 600}
                             :children []}}})
