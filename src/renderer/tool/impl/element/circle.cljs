(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :circle ::hierarchy/element)

(defmethod hierarchy/properties :circle
  []
  {:icon "circle-tool"})

(defmethod hierarchy/drag :circle
  [db]
  (let [offset (:adjusted-pointer-offset db)
        position (:adjusted-pointer-pos db)
        {:keys [stroke fill]} (get-in db [:documents (:active-document db)])
        [x y] offset
        radius (mat/distance position offset)
        attrs {:cx x
               :cy y
               :fill fill
               :stroke stroke
               :r radius}]
    (element.h/assoc-temp db {:type :element :tag :circle :attrs attrs})))
