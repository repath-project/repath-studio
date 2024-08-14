(ns renderer.element.db
  (:require
   [malli.core :as m]
   [renderer.tool.base :as tool]))


(defn tag?
  [tag]
  (contains? (descendants ::tool/element) tag))

(def element
  [:multi {:dispatch :tag}
   [::m/default
    [:map
     [:key keyword?]
     [:tag [:fn tag?]]
     [:parent {:optional true} keyword?]
     [:type [:enum {:default :element} :element :handle]]
     [:visible? [boolean? {:default true}]]
     [:locked? {:optional true} boolean?]
     [:selected? {:optional true} boolean?]
     [:children [:vector {:default []} keyword?]]
     [:bounds {:optional true} [:tuple double? double? double? double?]]
     [:content {:optional true} string?]
     [:attrs {:optional true} [:map-of keyword? [:or string? number?]]]]]])

(def elements
  [:map-of {:default {}} keyword? element])

(def valid? (m/validator element))
