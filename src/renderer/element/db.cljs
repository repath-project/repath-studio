(ns renderer.element.db
  (:require
   [malli.core :as m]
   [renderer.tool.base :as tool]))


(defn valid-tag?
  [tag]
  (contains? (descendants ::tool/element) tag))

(def element
  [:multi {:dispatch :tag}
   [::m/default
    [:map
     [:key keyword?]
     [:tag [:fn valid-tag?]]
     [:parent {:optional true} keyword?]
     [:type [:enum {:default :element} :element :handle]]
     [:visible? [boolean? {:default true}]]
     [:locked? {:optional true} [:maybe boolean?]]
     [:selected? {:optional true} boolean?]
     [:children {:optional true} [:vector {:default []} keyword?]]
     [:bounds {:optional true} [:tuple double? double? double? double?]]
     [:content {:optional true} string?]
     [:attrs {:optional true} map?]]]])

(def elements
  [:map-of {:default {}} keyword? element])

(def valid? (m/validator element))
