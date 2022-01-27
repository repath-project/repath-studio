(ns repath.studio.attrs.db
  (:require [cljs.spec.alpha :as s]))

(s/def ::positive-number (s/and number? #(>= % 0)))

(s/def ::width string?)

(s/def  ::stroke-linejoin string?)

(s/def ::length string?)