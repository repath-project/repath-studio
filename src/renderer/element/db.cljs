(ns renderer.element.db
  (:require
   [malli.core :as ma]))

(def element
  [:multi {:dispatch :tag}
   [::ma/default
    [:map
     [:key uuid?]
     [:type [:enum :element :handle]]
     [:visible? boolean?]
     [:selected? boolean?]
     [:attrs [:map-of :uuid string?]]]]])
