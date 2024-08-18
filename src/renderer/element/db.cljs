(ns renderer.element.db
  (:require
   [malli.core :as m]
   [renderer.tool.base :as tool]))


(defn tag?
  [k]
  (contains? (descendants ::tool/element) k))

(def tag
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tag"))}
   tag?])

(def bounds
  [:tuple number? number? number? number?])

(def element
  [:map
   [:key keyword?]
   [:tag tag]
   [:parent {:optional true} keyword?]
   [:type {:default :element} [:enum :element :handle]]
   [:visible? {:default true} boolean?]
   [:locked? {:optional true} boolean?]
   [:selected? {:optional true} boolean?]
   [:children {:default []} [:vector keyword?]]
   [:bounds {:optional true} bounds]
   [:content {:optional true} string?]
   [:attrs {:optional true} [:map-of keyword? [:or string? number? vector?]]]])

(def elements
  [:map-of {:default {}} keyword? element])

(def valid? (m/validator element))
