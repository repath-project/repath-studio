(ns renderer.utils.bounds
  (:require
   [clojure.core.matrix :as mat]
   [malli.core :as m]
   [renderer.snap.db :refer [SnapOptions]]
   [renderer.utils.dom :refer [DomElement]]
   [renderer.utils.math :refer [Vec2D]]))

(def Bounds
  "Coordinates that define a bounding box."
  [:tuple
   [number? {:title "left"}]
   [number? {:title "top"}]
   [number? {:title "right"}]
   [number? {:title "bottom"}]])

(m/=> from-bbox [:-> DomElement [:maybe Bounds]])
(defn from-bbox
  "Experimental way of getting the bounds of uknown or complicated elements
   using the getBBox method.
   https://developer.mozilla.org/en-US/docs/Web/API/SVGGraphicsElement/getBBox"
  [el]
  (when (.-getBBox el)
    (let [b (.getBBox el)
          x1 (.-x b)
          y1 (.-y b)
          x2 (+ x1 (.-width b))
          y2 (+ y1 (.-height b))]
      [x1 y1 x2 y2])))

(m/=> union [:-> [:+ Bounds] Bounds])
(defn union
  "Calculates the bounds that contain an arbitrary set of bounds."
  [& bounds]
  (vec (concat (apply map min (map #(take 2 %) bounds))
               (apply map max (map #(drop 2 %) bounds)))))

(m/=> ->dimensions [:-> Bounds Vec2D])
(defn ->dimensions
  "Converts bounds to [width heigh]"
  [[x1 y1 x2 y2]]
  (mat/sub [x2 y2] [x1 y1]))

(m/=> center [:-> Bounds Vec2D])
(defn center
  "Calculates the center of bounds."
  [b]
  (mat/add (take 2 b)
           (mat/div (->dimensions b) 2)))

(m/=> intersect? [:-> Bounds Bounds boolean?])
(defn intersect?
  "Tests whether the provided set of bounds intersect."
  [[a-left a-top a-right a-bottom] [b-left b-top b-right b-bottom]]
  (not (or (> b-left a-right)
           (< b-right a-left)
           (> b-top a-bottom)
           (< b-bottom a-top))))

(m/=> contained? [:-> Bounds Bounds boolean?])
(defn contained?
  "Tests whether `bounds-a` fully contain `bounds-b`."
  [[a-left a-top a-right a-bottom] [b-left b-top b-right b-bottom]]
  (and (> a-left b-left)
       (> a-top b-top)
       (< a-right b-right)
       (< a-bottom b-bottom)))

(m/=> contained-point? [:-> Bounds Vec2D boolean?])
(defn contained-point?
  "Tests whether the provided bounds contain a point."
  [[left top right bottom] [x y]]
  (and (<= left x)
       (<= top y)
       (>= right x)
       (>= bottom y)))

(m/=> ->snapping-points [:-> Bounds SnapOptions [:* Vec2D]])
(defn ->snapping-points
  [bounds options]
  (let [[x1 y1 x2 y2] bounds
        [cx cy] (center bounds)]
    (cond-> []
      (:corners options)
      (into [[x1 y1]
             [x1 y2]
             [x2 y1]
             [x2 y2]])

      (:centers options)
      (into [[cx cy]])

      (:midpoints options)
      (into [[x1 cy]
             [x2 cy]
             [cx y1]
             [cx y2]]))))
