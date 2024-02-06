(ns renderer.utils.math
  (:require
   [clojure.math :as math]))

(defn clamp
  [x minimum maximum]
  (min (max x minimum) maximum))

(defn angle-dx
  [degrees radius]
  (* radius (Math/cos (math/to-radians degrees))))

(defn angle-dy
  [degrees radius]
  (* radius (Math/sin (math/to-radians degrees))))
