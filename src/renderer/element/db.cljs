(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :refer [bounds]]))

(defn tag?
  [k]
  (contains? (descendants ::tool/element) k))

(def tag
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tag"))}
   tag?])

(def attr
  [:or string? number? vector? nil?])

(def attrs
  [:map-of keyword? attr])

(def handle
  [:map {:closed true}
   [:id keyword?]
   [:tag [:enum :move :scale :edit]]
   [:type [:= :handle]]
   [:cursor {:optional true} string?]
   [:x {:optional true} number?]
   [:y {:optional true} number?]
   [:size {:optional true} number?]
   [:stroke-width {:optional true} number?]
   [:element {:optional true} uuid?]])

(def element
  [:map {:closed true}
   [:id {:optional true} uuid?]
   [:tag tag]
   [:label {:optional true} string?]
   [:parent {:optional true} uuid?]
   [:type {:optional true} [:= :element]]
   [:visible? {:optional true} boolean?]
   [:locked? {:optional true} boolean?]
   [:selected? {:optional true} boolean?]
   [:children {:default [] :optional true} [:vector uuid?]]
   [:bounds {:optional true} bounds]
   [:content {:optional true} string?]
   [:attrs {:optional true} attrs]])

(def elements
  [:map-of {:default {}} uuid? element])

(def valid? (m/validator element))

(def explain (m/explainer element))

(def explain-elements (m/explainer elements))

(def default (m/decode element {:type :element
                                :visible? true
                                :children []} mt/default-value-transformer))
