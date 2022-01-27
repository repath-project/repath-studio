(ns repath.studio.tools.group
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]))

(derive :g ::tools/container)

(defmethod tools/properties :g [] {:description "The <g> SVG element is a container used to group other SVG elements."})

(defmethod tools/move :g
  [element [x y]] element)