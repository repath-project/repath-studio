(ns repath.studio.elements.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::bounds (s/cat :start ::vec2? :end ::vec2?))
(s/def ::attr (s/map-of keyword? string?))
(s/def ::attrs (s/coll-of ::attr?))
(s/def ::key keyword?)
(s/def ::id string?)
(s/def ::name string?)
(s/def ::visible? boolean?)
(s/def ::locked? boolean?)
(s/def ::type keyword?)

(defmulti element-type ::type)

(defmethod element-type :default [_]
  (s/keys :req-un [::key ::type ::visible?]
          :opt-un [::name]))

(s/def ::element (s/multi-spec element-type ::type))
