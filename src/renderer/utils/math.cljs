(ns renderer.utils.math
  (:require
   [clojure.math :as math]
   [malli.core :as m]))

(def Vec2
  [:tuple number? number?])

(m/=> clamp [:-> number? number? number? number?])
(defn clamp
  "Clamps a number within the provided bounds."
  [x minimum maximum]
  (min (max x minimum) maximum))

(m/=> angle-dx [:-> number? number? number?])
(defn angle-dx
  [degrees radius]
  (* radius (Math/cos (math/to-radians degrees))))

(m/=> angle-dy [:-> number? number? number?])
(defn angle-dy
  [degrees radius]
  (* radius (Math/sin (math/to-radians degrees))))

(m/=> normalize-angle [:-> number? number?])
(defn normalize-angle
  "Normalizes an angle to be in range [0-360). Angles outside this range will
   be normalized to be the equivalent angle with that range."
  [angle]
  (mod angle (* 2 Math/PI)))

(m/=> angle [:-> Vec2 Vec2 number?])
(defn angle
  "Calculates the angle between two points."
  [[x1 y1] [x2 y2]]
  (-> (Math/atan2 (- y2 y1) (- x2 x1))
      (normalize-angle)
      (math/to-degrees)))
