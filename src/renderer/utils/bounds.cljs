(ns renderer.utils.bounds
  (:require
   [clojure.core.matrix :as matrix]
   [malli.core :as m]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.math :refer [Vec2]]))

(def BBox
  [:tuple
   [number? {:title "min-x"}]
   [number? {:title "min-y"}]
   [number? {:title "max-x"}]
   [number? {:title "max-y"}]])

(def DomElement
  [:fn (fn [x] (instance? js/Element x))])

(m/=> dom-el->bbox [:-> DomElement [:maybe BBox]])
(defn dom-el->bbox
  "Experimental way of getting the bounds of unknown or complicated elements
   using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [el]
  (when (.-getBBox el)
    (let [b (.getBBox el)
          min-x (.-x b)
          min-y (.-y b)
          max-x (+ min-x (.-width b))
          max-y (+ min-y (.-height b))]
      [min-x min-y max-x max-y])))

(m/=> union [:-> [:+ BBox] BBox])
(defn union
  "Returns the bounding box that contains the provided collection of bounds."
  [& bbox]
  (vec (concat (apply map min (map #(take 2 %) bbox))
               (apply map max (map #(drop 2 %) bbox)))))

(m/=> ->dimensions [:-> BBox Vec2])
(defn ->dimensions
  "Converts a bounding box to [width height]."
  [[min-x min-y max-x max-y]]
  (matrix/sub [max-x max-y] [min-x min-y]))

(m/=> center [:-> BBox Vec2])
(defn center
  "Calculates the center of a bounding box."
  [bbox]
  (matrix/add (take 2 bbox)
              (matrix/div (->dimensions bbox) 2)))

(m/=> intersect? [:-> BBox BBox boolean?])
(defn intersect?
  "Tests whether the provided set of bounds intersect."
  [[a-min-x a-min-y a-max-x a-max-y]
   [b-min-x b-min-y b-max-x b-max-y]]
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
        [cx cy] (center bbox)]
    (cond-> []
      (:corners options)
      (into [(with-meta [min-x min-y] {:label (t [::bounds-corner "bounds corner"])})
             (with-meta [min-x max-y] {:label (t [::bounds-corner "bounds corner"])})
             (with-meta [max-x min-y] {:label (t [::bounds-corner "bounds corner"])})
             (with-meta [max-x max-y] {:label (t [::bounds-corner "bounds corner"])})])

      (:centers options)
      (into [(with-meta [cx cy] {:label (t [::bounds-center "bounds center"])})])

      (:midpoints options)
      (into [(with-meta [min-x cy] {:label (t [::bounds-corner "bounds midpoint"])})
             (with-meta [max-x cy] {:label (t [::bounds-corner "bounds midpoint"])})
             (with-meta [cx min-y] {:label (t [::bounds-corner "bounds midpoint"])})
             (with-meta [cx max-y] {:label (t [::bounds-corner "bounds midpoint"])})]))))
