(ns renderer.utils.math
  (:require
   [clojure.math :as math]
   [malli.experimental :as mx]))

(def Vec2D
  [:tuple number? number?])

(mx/defn clamp :- number?
  "Clamps a number within the provided bounds."
  [x :- number?, minimum :- number?, maximum :- number?]
  (min (max x minimum) maximum))

(mx/defn angle-dx :- number?
  [degrees :- number?, radius :- number?]
  (* radius (Math/cos (math/to-radians degrees))))

(mx/defn angle-dy :- number?
  [degrees :- number?, radius :- number?]
  (* radius (Math/sin (math/to-radians degrees))))

(mx/defn normalize-angle :- number?
  "Normalizes an angle to be in range [0-360). Angles outside this range will
   be normalized to be the equivalent angle with that range."
  [angle :- number?]
  (mod angle (* 2 Math/PI)))

(mx/defn angle :- number?
  "Calculates the angle between two points."
  [[x1 y1] :- Vec2D, [x2 y2] :- Vec2D]
  (-> (Math/atan2 (- y2 y1) (- x2 x1))
      (normalize-angle)
      (math/to-degrees)))


