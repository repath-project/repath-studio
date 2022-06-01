(ns repath.studio.tools.svg
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]))

(derive :svg ::tools/container)

(defmethod tools/properties :svg [] {:icon "svg"
                                     :description "The svg element is a container that defines a new coordinate system and viewport. 
                                                  It is used as the outermost element of SVG documents, but it can also be used to embed an SVG fragment inside an SVG or HTML document."
                                     :attrs [:title
                                             :overflow]})

(defmethod tools/drag :svg
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))}]
    (elements/set-temp db {:type :element :tag :svg :attrs attrs})))

