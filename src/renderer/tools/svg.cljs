(ns renderer.tools.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.tools.base :as tools]))

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
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))}]
    (element.h/set-temp db {:type :element :tag :svg :attrs attrs})))