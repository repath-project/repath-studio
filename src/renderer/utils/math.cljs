(ns renderer.utils.math
  (:require
   [clojure.math :as math]))

(defn clamp
  "Clamps a number within the provided bounds."
  [x minimum maximum]
  (min (max x minimum) maximum))

(defn angle-dx
  [degrees radius]
  (* radius (Math/cos (math/to-radians degrees))))

(defn angle-dy
  [degrees radius]
  (* radius (Math/sin (math/to-radians degrees))))

(defn normalize-angle
  "Normalizes an angle to be in range [0-360). Angles outside this range will
   be normalized to be the equivalent angle with that range."
  [angle]
  (mod angle (* 2 Math/PI)))

(defn angle
  "Calculates the angle between two points."
  [x1 y1 x2 y2]
  (-> (Math/atan2 (- y2 y1) (- x2 x1))
      normalize-angle
      math/to-degrees))


