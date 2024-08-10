(ns renderer.element.db
  (:require
   [malli.core :as ma]))

(def element
  [:multi {:dispatch :tag}
   [::ma/default
    [:map
     [:key keyword?]
     [:parent {:optional true} keyword?]
     [:type [:enum :element :handle]]
     [:visible? boolean?]
     [:locked? {:optional true} boolean?]
     [:selected? {:optional true} boolean?]
     [:children [:vector keyword?]]
     [:bounds {:optional true} [:tuple double? double? double? double?]]
     [:content {:optional true} string?]
     [:attrs {:optional true} map?]]]])
