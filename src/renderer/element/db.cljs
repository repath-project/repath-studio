(ns renderer.element.db
  (:require
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.utils.bounds :refer [BBox]]))

(defn tag?
  [k]
  (contains? (descendants ::element.hierarchy/element) k))

(def Tag
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", is not a supported tag"))}
   tag?])

(def image-mime-types
  {"image/png" [".png"]
   "image/jpeg" [".jpeg" ".jpg"]
   "image/bmp" [".bmp"]
   "image/gif" [".gif"]
   "image/webp" [".webp"]})

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
   [:bbox {:optional true} BBox]
   [:content {:optional true} string?]
   [:attrs {:optional true} Attrs]])

(def valid? (m/validator Element))

(def explain (m/explainer Element))

(def default (m/decode Element {:type :element
                                :visible true
                                :children []} m.transform/default-value-transformer))
