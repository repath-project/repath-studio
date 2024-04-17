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
   [:filter keyword?]
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
   :pan [0 0]
   :history {:zoom 0.5}
   :elements {:canvas {:key :canvas
                       :visible? true
                       :tag :canvas
                       :type :element
                       :attrs {:fill "#eeeeee"}
                       :children []}}})
