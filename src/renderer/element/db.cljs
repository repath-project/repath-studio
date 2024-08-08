(ns renderer.element.db
  (:require
   [malli.core :as ma]))

(def element
  [:multi {:dispatch :tag}
   [::ma/default
    [:map
     [:key keyword?]
     [:type [:enum :element :handle]]
     [:visible? boolean?]
     [:selected? boolean?]
     [:content string?]
     [:attrs [:map-of keyword? string?]]]]])
