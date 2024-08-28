(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.tool.base :as tool]
   [renderer.utils.bounds :as bounds]))

(defn tag?
  [k]
  (contains? (descendants ::tool/element) k))

(def tag
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tag"))}
   tag?])

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
   [:element {:optional true} keyword?]])

(def element
  [:map {:closed true}
   [:id keyword?]
   [:tag tag]
   [:label {:optional true} string?]
   [:parent {:optional true} keyword?]
   [:type {:default :element} [:= :element]]
   [:visible? {:default true} boolean?]
   [:locked? {:optional true} boolean?]
   [:selected? {:optional true} boolean?]
   [:children {:default []} [:vector keyword?]]
   [:bounds {:optional true} bounds/bounds]
   [:content {:optional true} string?]
   [:attrs {:optional true} [:map-of keyword? [:or string? number? vector?]]]])

(def elements
  [:map-of {:default {}} keyword? element])

(def valid? (m/validator element))

(def default (m/decode element {} mt/default-value-transformer))
