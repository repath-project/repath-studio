(ns renderer.utils.bounds
  (:require
   [clojure.core.matrix :as matrix]))

(defn union
  "Calculates the wrapping bounds of bounds."
  [[ax1 ay1 ax2 ay2] [bx1 by1 bx2 by2]]
  [(min ax1 bx1) (min ay1 by1) (max ax2 bx2) (max ay2 by2)])

(defn ->dimensions
  "Converts bounds to a [width heigh] vector."
  [[x1 y1 x2 y2]]
  (matrix/sub [x2 y2] [x1 y1]))

(defn intersect-bounds?
  "Checks if bounds intersect."
  [a-bounds b-bounds]
  (if (and a-bounds b-bounds)
    (let [[a-left a-top a-right a-bottom] a-bounds
          [b-left b-top b-right b-bottom] b-bounds]
      (not (or (> b-left a-right)
               (< b-right a-left)
               (> b-top a-bottom)
               (< b-bottom a-top)))) false))

(defn contain-bounds?
  [a-bounds b-bounds]
  (if (and a-bounds b-bounds)
    (let [[a-left a-top a-right a-bottom] a-bounds
          [b-left b-top b-right b-bottom] b-bounds]
      (and (> a-left b-left)
           (> a-top b-top)
           (< a-right b-right)
           (< a-bottom b-bottom))) false))

(defn contain-point?
  [[left top right bottom] [x y]]
  (and (<= left x)
       (<= top y)
       (>= right x)
       (>= bottom y)))