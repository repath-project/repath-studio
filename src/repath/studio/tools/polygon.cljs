(ns repath.studio.tools.polygon
  (:require [repath.studio.tools.base :as tools]))

(derive :polygon ::tools/shape)

(defmethod tools/properties :polygon [] {:icon "polygon"})
