(ns renderer.utils.bounds
  (:require
   [malli.core :as m]
   [renderer.db :refer [JS_Element]]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.math :refer [Vec2]]))

(def BBox
  [:tuple
   [number? {:title "min-x"}]
   [number? {:title "min-y"}]
   [number? {:title "max-x"}]
   [number? {:title "max-y"}]])

(m/=> dom-el->bbox [:-> JS_Element [:maybe BBox]])
(defn dom-el->bbox
  "Experimental way of getting the bounds using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [dom-el]
  (when (.-getBBox dom-el)
    (let [b (.getBBox dom-el)
          min-x (.-x b)
          min-y (.-y b)
          max-x (+ min-x (.-width b))
          max-y (+ min-y (.-height b))]
      [min-x min-y max-x max-y])))

(m/=> union [:-> [:+ BBox] BBox])
(defn union
  "Returns the bounding box that contains the provided collection of bounds."
  [& bbox]
  (reduce (fn [[a-min-x a-min-y a-max-x a-max-y] [b-min-x b-min-y b-max-x b-max-y]]
            [(min a-min-x b-min-x)
             (min a-min-y b-min-y)
             (max a-max-x b-max-x)
             (max a-max-y b-max-y)])
          bbox))

(m/=> ->dimensions [:-> BBox Vec2])
(defn ->dimensions
  "Converts a bounding box to [width height]."
  [[min-x min-y max-x max-y]]
  [(- max-x min-x) (- max-y min-y)])

(m/=> center [:-> BBox Vec2])
(defn center
  "Calculates the center of a bounding box."
  [[min-x min-y max-x max-y]]
  [(+ min-x (/ (- max-x min-x) 2))
   (+ min-y (/ (- max-y min-y) 2))])

(m/=> intersect? [:-> BBox BBox boolean?])
(defn intersect?
  "Tests whether the provided set of bounds intersect."
  [[a-min-x a-min-y a-max-x a-max-y] [b-min-x b-min-y b-max-x b-max-y]]
  (not (or (> b-min-x a-max-x)
           (< b-max-x a-min-x)
           (> b-min-y a-max-y)
           (< b-max-y a-min-y))))

(m/=> contained? [:-> BBox BBox boolean?])
(defn contained?
  "Tests whether `bounds-a` fully contain `bounds-b`."
  [[a-min-x a-min-y a-max-x a-max-y] [b-min-x b-min-y b-max-x b-max-y]]
  (and (> a-min-x b-min-x)
       (> a-min-y b-min-y)
       (< a-max-x b-max-x)
       (< a-max-y b-max-y)))

(m/=> contained-point? [:-> BBox Vec2 boolean?])
(defn contained-point?
  "Tests whether the provided bounding box contains a point."
  [[min-x min-y max-x b-max-y] [x y]]
  (and (<= min-x x)
       (<= min-y y)
       (>= max-x x)
       (>= b-max-y y)))

(m/=> ->snapping-points [:-> BBox SnapOptions [:* Vec2]])
(defn ->snapping-points
  [bbox options]
  (let [[min-x min-y max-x max-y] bbox
        [cx cy] (center bbox)
        bounds-corner-txt #(t [::bounds-corner "bounds corner"])
        bounds-center-txt #(t [::bounds-center "bounds center"])
        bounds-midpoints-txt #(t [::bounds-midpoint "bounds midpoint"])]
    (cond-> []
      (:corners options)
      (into [(with-meta [min-x min-y] {:label bounds-corner-txt})
             (with-meta [min-x max-y] {:label bounds-corner-txt})
             (with-meta [max-x min-y] {:label bounds-corner-txt})
             (with-meta [max-x max-y] {:label bounds-corner-txt})])

      (:centers options)
      (into [(with-meta [cx cy] {:label bounds-center-txt})])

      (:midpoints options)
      (into [(with-meta [min-x cy] {:label bounds-midpoints-txt})
             (with-meta [max-x cy] {:label bounds-midpoints-txt})
             (with-meta [cx min-y] {:label bounds-midpoints-txt})
             (with-meta [cx max-y] {:label bounds-midpoints-txt})]))))
