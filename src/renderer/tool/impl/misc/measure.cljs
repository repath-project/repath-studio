(ns renderer.tool.impl.misc.measure
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.handlers :as app.h]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :measure ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :measure
  []
  {:icon "ruler-triangle"})

(defmethod tool.hierarchy/help [:measure :default]
  []
  "Click and drag to measure a distance.")

(defmethod tool.hierarchy/activate :measure
  [db]
  (app.h/set-cursor db "crosshair"))

(defmethod tool.hierarchy/pointer-up :measure
  [db]
  (element.h/dissoc-temp db))

(defmethod tool.hierarchy/drag-end :measure
  [db] db)

(defmethod tool.hierarchy/drag :measure
  [db]
  (let [{:keys [adjusted-pointer-offset adjusted-pointer-pos]} db
        [offset-x offset-y] adjusted-pointer-offset
        [x y] adjusted-pointer-pos
        [adjacent opposite] (mat/sub adjusted-pointer-offset adjusted-pointer-pos)
        hypotenuse (Math/hypot adjacent opposite)
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 x
               :y2 y
               :stroke "gray"}]
    (element.h/assoc-temp db {:id :mesure
                              :type :overlay
                              :tag :measure
                              :attrs attrs
                              :hypotenuse hypotenuse})))
