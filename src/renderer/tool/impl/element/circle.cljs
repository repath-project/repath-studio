(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [renderer.document.handlers :as document.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :circle ::hierarchy/element)

(defmethod hierarchy/properties :circle
  []
  {:icon "circle-tool"})

(defmethod hierarchy/drag :circle
  [db]
  (let [offset (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        position (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        [x y] offset
        radius (mat/distance position offset)
        attrs {:cx x
               :cy y
               :fill (document.h/attr db :fill)
               :stroke (document.h/attr db :stroke)
               :r radius}]
    (h/set-temp db {:type :element :tag :circle :attrs attrs})))

(defmethod hierarchy/snapping-bases :circle
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str (name (:tool db)) " " (if (h/temp db) "radius" "center"))})])
