(ns renderer.element.hierarchy)

;; REVIEW: Is this type of complexity really needed?
(derive ::graphics ::renderable)
(derive ::gradient ::renderable)
(derive ::descriptive ::renderable)
(derive :foreignObject ::graphics)
(derive :textPath ::graphics)
(derive :tspan ::graphics)
(derive :linearGradient ::gradient)
(derive :radialGradient ::gradient)
(derive :desc ::descriptive)
(derive :metadata ::descriptive)
(derive :title ::descriptive)

(defmulti render :tag)
(defmulti render-to-string :tag)
(defmulti render-edit :tag)
(defmulti path :tag)
(defmulti area :tag)
(defmulti centroid :tag)
(defmulti snapping-points :tag)
(defmulti bbox :tag)
(defmulti translate (fn [el _offset] (:tag el)))
(defmulti scale (fn [el _ratio _pivot-point] (:tag el)))
(defmulti edit (fn [el _offset _handle] (:tag el)))
(defmulti properties identity)

(defmethod render :default [])
(defmethod render-to-string :default [el] [render el])
(defmethod render-edit :default [])
(defmethod bbox :default [])
(defmethod area :default [])
(defmethod centroid :default [])
(defmethod snapping-points :default [])
(defmethod translate :default [el _offset] el)
(defmethod scale :default [el _ratio _pivot-point] el)
(defmethod edit :default [el _offset _handle] el)
(defmethod properties :default [])
