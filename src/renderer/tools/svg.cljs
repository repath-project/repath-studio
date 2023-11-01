(ns renderer.tools.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [renderer.element.handlers :as elements]
   [renderer.tools.base :as tools]
   [clojure.core.matrix :as matrix]
   [renderer.utils.units :as units]
   [clojure.string :as str]))

(derive :svg ::tools/container)

(defmethod tools/properties :svg
  []
  {;; :icon "svg"
   :description "The svg element is a container that defines a new coordinate 
                 system and viewport. It is used as the outermost element of 
                 SVG documents, but it can also be used to embed an SVG fragment 
                 inside an SVG or HTML document."
   :attrs [:title
           :overflow]})

(defmethod tools/drag :svg
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))}]
    (elements/set-temp db {:type :element :tag :svg :attrs attrs})))

(defmethod tools/bounds :svg
  [{:keys [attrs]}]
  (let [{:keys [x y width height stroke-width stroke]} attrs
        [x y width height stroke-width-px] (mapv units/unit->px
                                                 [x y width height stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
        [x y] (matrix/sub [x y]
                          (/ (if (str/blank? stroke) 0 stroke-width-px) 2))
        [width height] (matrix/add [width height]
                                   (if (str/blank? stroke) 0 stroke-width-px))]
    (mapv units/unit->px [x y (+ x width) (+ y height)])))
