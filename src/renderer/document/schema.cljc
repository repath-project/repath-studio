(ns renderer.document.schema
  (:require
   [renderer.elements.schema]))

(def document
  [:map
   [:hovered-keys [:set uuid?]]
   [:ignored-keys [:set uuid?]]
   [:active-page uuid?]
   [:rulers-locked? boolean?]
   [:grid? boolean?]
   [:rulers? boolean?]
   [:snap? boolean?]
   [:fill string?]
   [:stroke string?]
   [:zoom double?]
   [:rotate double?]
   [:filter string?]
   [:pan [:tuple double? double?]]
   [:elements [:map-of :uuid renderer.elements.schema/element]]])