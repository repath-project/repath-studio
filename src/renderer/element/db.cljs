(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.bounds :refer [Bounds]]))

(defn tag?
  [k]
  (contains? (descendants ::tool.hierarchy/element) k))

(def Tag
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tag"))}
   tag?])

(def Attr
  [:or string? number? vector? nil?]) ; REVIEW

(def Attrs
  [:map-of keyword? Attr])

(def Handle
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

(def Element
  [:map {:closed true}
   [:id {:optional true} uuid?]
   [:tag Tag]
   [:label {:optional true} string?]
   [:parent {:optional true} uuid?]
   [:type {:optional true} [:= :element]]
   [:visible {:optional true} boolean?]
   [:locked {:optional true} boolean?]
   [:selected {:optional true} boolean?]
   [:children {:default [] :optional true} [:vector uuid?]]
   [:bounds {:optional true} Bounds]
   [:content {:optional true} string?]
   [:attrs {:optional true} Attrs]])

(def valid? (m/validator Element))

(def explain (m/explainer Element))

(def default (m/decode Element {:type :element
                                :visible true
                                :children []} mt/default-value-transformer))
