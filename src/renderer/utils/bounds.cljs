(ns renderer.utils.bounds
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.utils.math :as math]))

(def bounds [:tuple number? number? number? number?])

(mx/defn from-bbox :- bounds
  "Experimental way of getting the bounds of uknown or complicated elements
   using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [^js/Element el]
  (when (.-getBBox el)
    (let [bounds (.getBBox el)
          x1 (.-x bounds)
          y1 (.-y bounds)
          x2 (+ x1 (.-width bounds))
          y2 (+ y1 (.-height bounds))]
      [x1 y1 x2 y2])))

(mx/defn union :- bounds
  "Calculates the bounds that contain an arbitrary set of bounds."
  [& bounds :- bounds]
  (vec (concat (apply map min (map #(take 2 %) bounds))
               (apply map max (map #(drop 2 %) bounds)))))

(mx/defn ->dimensions :- [:tuple number? number?]
  "Converts bounds to [width heigh]"
  [[x1 y1 x2 y2] :- bounds]
  (mat/sub [x2 y2] [x1 y1]))

(mx/defn center :- math/point
  "Calculates the center of bounds."
  [bounds :- bounds]
  (mat/add (take 2 bounds)
           (mat/div (->dimensions bounds) 2)))

(mx/defn intersect? :- boolean?
  "Tests whether the provided set of bounds intersect."
  [a-bounds :- bounds
   b-bounds :- bounds]
  (when (and a-bounds b-bounds)
    (let [[a-left a-top a-right a-bottom] a-bounds
          [b-left b-top b-right b-bottom] b-bounds]
      (not (or (> b-left a-right)
               (< b-right a-left)
               (> b-top a-bottom)
               (< b-bottom a-top))))))

(mx/defn contained? :- boolean?
  "Tests whether `bounds-a` fully contain `bounds-b`."
  [a-bounds :- bounds
   b-bounds :- bounds]
  (when (and a-bounds b-bounds)
    (let [[a-left a-top a-right a-bottom] a-bounds
          [b-left b-top b-right b-bottom] b-bounds]
      (and (> a-left b-left)
           (> a-top b-top)
           (< a-right b-right)
           (< a-bottom b-bottom)))))

(mx/defn contain-point? :- boolean?
  "Tests whether the provided bounds contain a point."
  [[left top right bottom] :- bounds
   [x y] :- math/point]
  (and (<= left x)
       (<= top y)
       (>= right x)
       (>= bottom y)))
