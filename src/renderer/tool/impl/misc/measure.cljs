(ns renderer.tool.impl.misc.measure
  (:require
   [clojure.core.matrix :as mat]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :measure ::hierarchy/tool)

(defmethod hierarchy/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod hierarchy/help [:measure :idle]
  []
  "Click and drag to measure a distance.")

(defmethod hierarchy/activate :measure
  [db]
  (h/set-cursor db "crosshair"))

(defmethod hierarchy/pointer-up :measure
  [db]
  (h/dissoc-temp db))

(defmethod hierarchy/drag-end :measure [db] db)

(defmethod hierarchy/snapping-bases :measure
  [db]
  [(:adjusted-pointer-pos db)])

(defmethod hierarchy/drag :measure
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        [adjacent opposite] (mat/sub [offset-x offset-y] [x y])
        hypotenuse (Math/hypot adjacent opposite)]
    (h/set-temp db {:type :element
                    :tag :measure
                    :attrs {:x1 offset-x
                            :y1 offset-y
                            :x2 x
                            :y2 y
                            :hypotenuse hypotenuse
                            :stroke "gray"}})))
