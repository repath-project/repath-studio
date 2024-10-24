(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.bounds :refer [Bounds]]))

(defn tag?
  [k]
  (contains? (descendants ::hierarchy/element) k))

(def Tag
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tag"))}
   tag?])

(def AnimationTag
  [:enum :animate :animateTransform :animateMotion])

(def Attrs
  [:map-of keyword? string?])

(def Direction
  [:enum :top :center-vertical :bottom :left :center-horizontal :right])

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
