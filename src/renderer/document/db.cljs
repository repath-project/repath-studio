(ns renderer.document.db
  (:require
   [renderer.element.db]
   [renderer.history.db]
   [renderer.utils.uuid :as uuid]))

(def document
  [:map
   [:key keyword?]
   [:title string?]
   [:hovered-keys [:set keyword?]]
   [:collapsed-keys [:set keyword?]]
   [:ignored-keys [:set keyword?]]
   [:fill string?]
   [:stroke string?]
   [:zoom double?]
   [:rotate double?]
   [:history renderer.history.db/history]
   [:pan [:tuple double? double?]]
   [:elements [:map-of keyword? renderer.element.db/element]]])

(defn create-document
  []
  (let [id (uuid/generate)]
    {:hovered-keys #{}
     :collapsed-keys #{}
     :ignored-keys #{}
     :fill "white"
     :stroke "black"
     :zoom 1
     :rotate 0
     :pan [0 0]
     :history {:zoom 0.5}
     :elements {id {:key id
                    :visible? true
                    :tag :canvas
                    :type :element
                    :attrs {:fill "#eeeeee"}
                    :children []}}}))
