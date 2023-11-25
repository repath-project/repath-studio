(ns renderer.utils.bounds
  (:require
   [clojure.core.matrix :as mat]))

(defn union
  "Calculates the union of bounds."
  [& bounds]
  (concat (apply map min (map #(take 2 %) bounds))
          (apply map max (map #(drop 2 %) bounds))))

(apply union [[-100 -100 100 100]] )

(defn ->dimensions
  "Converts bounds to a [width heigh] vector."
  [[x1 y1 x2 y2]]
  (mat/sub [x2 y2] [x1 y1]))

(defn center
  [bounds]
  (let [[x1 y1 _x2 _y2] bounds]
    (mat/add [x1 y1] (mat/div (->dimensions bounds) 2))))

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
