(ns renderer.attribute.utils
    (:require
    [clojure.string :as str]))

(defn points-to-vec
  [points]
  (vec (as-> points p
         (str/triml p)
         (str/split p #"\s+")
         (partition 2 p))))