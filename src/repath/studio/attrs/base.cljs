(ns repath.studio.attrs.base
  (:require
   [repath.studio.units :as units]
   [clojure.string :as str]))

(derive ::color ::attr)
(derive ::length ::attr)
(derive ::radius ::attr)

(derive :fill ::color)
(derive :stroke ::color)

(derive ::coordinate ::length)

(derive :x ::coordinate)
(derive :y ::coordinate)
(derive :x1 ::coordinate)
(derive :y1 ::coordinate)
(derive :x2 ::coordinate)
(derive :y2 ::coordinate)
(derive :cx ::coordinate)
(derive :cy ::coordinate)
(derive :dx ::coordinate)
(derive :dy ::coordinate)
(derive :x ::coordinate)
(derive :x ::coordinate)
(derive :x ::coordinate)

(derive :width ::length)
(derive :height ::length)
(derive :stroke-width ::length)
(derive :r ::length)

(derive :rx ::radius)
(derive :ry ::radius)

(defmulti update-attr (fn [_ attr] attr))

(defmethod update-attr :default
  [element attribute f & args]
  (update-in element [:attrs attribute] f (first args)))

(defmethod update-attr ::length
  [element attribute f & args]
  (update-in element [:attrs attribute] #(units/transform f (first args) %)))

(defn points-to-vec
  [points]
  (as-> points p
    (str/triml p)
    (str/split p #"\s+")
    (partition 2 p)))