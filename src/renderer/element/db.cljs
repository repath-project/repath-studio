(ns renderer.element.db
  (:require
   [malli.core :as m]))

(def element
  [:multi {:dispatch :tag}
   [::m/default
    [:map
     [:key keyword?]
     [:parent {:optional true} keyword?]
     [:type [:enum {:default :element} :element :handle]]
     [:visible? [boolean? {:default true}]]
     [:locked? {:optional true} boolean?]
     [:selected? {:optional true} boolean?]
     [:children [:vector {:default []} keyword?]]
     [:bounds {:optional true} [:tuple double? double? double? double?]]
     [:content {:optional true} string?]
     [:attrs {:optional true} map?]]]])
