(ns renderer.utils.math)

(defn clamp
  [x minimum maximum]
  (min (max x minimum) maximum))
