(ns renderer.element.db
  (:require
   [malli.core :as m]
   [renderer.tool.base :as tool]))


(defn tag?
  [tag]
  (contains? (descendants ::tool/element) tag))

(def bounds
  [:tuple number? number? number? number?])

(def attr
  [:multi {:dispatch :tag}
   [::m/default
    ;; REVIEW: Attribute type should probably be a string.
    [:or string? number? vector?]]])

(def element
  [:map
   [:key keyword?]
   [:tag [:fn tag?]]
   [:parent {:optional true} keyword?]
   [:type [:enum {:default :element} :element :handle]]
   [:visible? [boolean? {:default true}]]
   [:locked? {:optional true} boolean?]
   [:selected? {:optional true} boolean?]
   [:children [:vector {:default []} keyword?]]
   [:bounds {:optional true} bounds]
   [:content {:optional true} string?]
   [:attrs {:optional true} [:map-of keyword? attr]]])

(def elements
  [:map-of {:default {}} keyword? element])

(def valid? (m/validator element))
