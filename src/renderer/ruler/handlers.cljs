(ns renderer.ruler.handlers
  (:require
   [clojure.math.combinatorics :as combo]
   [malli.core :as m]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.frame.db :refer [Viewbox]]
   [renderer.frame.handlers :as frame.h]
   [renderer.ruler.db :refer [Orientation]]))

(m/=> step [:-> ZoomFactor number?])
(defn step
  "Returns the grid step given a zoom level.

   Any attempt to ingeniously produce this mapping was proven inferior.
   Simply returning a fixed step depending on the zoom range works fine.
   Zoom levels outside of this range are considered invalid for now.
   Maybe we need to revisit this at some point."
  [zoom]
  (condp > zoom
    0.001 2000
    0.025 1000
    0.05 500
    0.1 200
    0.25 100
    0.5 50
    1 15
    2 10
    5 5
    10 2
    25 1
    50 0.5
    0.1))

(m/=> steps-coll [:-> number? Viewbox Orientation [:sequential number?]])
(defn steps-coll
  "Returns a collection of steps per orientation, contained on the given viewbox."
  [ruler-step viewbox orientation]
  (let [[x y width height] viewbox
        sections 10]
    (range (- (+ (* sections ruler-step)
                 (rem (if (= orientation :vertical) y x)
                      (* sections ruler-step))))
           (if (= orientation :vertical) height width)
           ruler-step)))

(defn steps-intersections
  "Returns the intersection points of the rulers."
  [db]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        viewbox (frame.h/viewbox db)
        ruler-step (step zoom)]
    (combo/cartesian-product (steps-coll ruler-step viewbox :vertical)
                             (steps-coll ruler-step viewbox :horizontal))))
