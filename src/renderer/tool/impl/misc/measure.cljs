(ns renderer.tool.impl.misc.measure
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.element.handlers :as element.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :measure ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod tool.hierarchy/help [:measure :idle]
  []
  "Click and drag to measure a distance.")

(defmethod tool.hierarchy/on-activate :measure
  [db]
  (tool.handlers/set-cursor db "crosshair"))

(defmethod tool.hierarchy/on-deactivate :measure
  [db]
  (tool.handlers/dissoc-temp db))

(defmethod tool.hierarchy/on-drag :measure
  [db _e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        [adjacent opposite] (matrix/sub [offset-x offset-y] [x y])
        hypotenuse (Math/hypot adjacent opposite)]
    (tool.handlers/set-temp db {:type :element
                                :tag :measure
                                :attrs {:x1 offset-x
                                        :y1 offset-y
                                        :x2 x
                                        :y2 y
                                        :hypotenuse hypotenuse
                                        :stroke "gray"}})))

(defmethod tool.hierarchy/snapping-points :measure
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str "measure " (if (tool.handlers/temp db) "end" "start"))})])

(defmethod tool.hierarchy/snapping-elements :measure
  [db]
  (filter :visible (vals (element.handlers/entities db))))
