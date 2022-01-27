(ns repath.studio.tools.polyline
  (:require [repath.studio.tools.base :as tools]))

(derive :polyline ::tools/shape)

(defmethod tools/properties :polyline [] {:icon "polyline"})
