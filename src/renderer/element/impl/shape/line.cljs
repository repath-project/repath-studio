(ns renderer.element.impl.shape.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive :line ::hierarchy/shape)

(defmethod hierarchy/properties :line
  []
  {:icon "line"
   :description "The <line> element is an SVG basic shape used to create a line
                 connecting two points."
   :attrs [:stroke
           :stroke-width
           :stroke-linecap
           :stroke-dasharray
           :opacity]})

(defmethod hierarchy/translate :line
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :x1 + x)
      (attr.hierarchy/update-attr :y1 + y)
      (attr.hierarchy/update-attr :x2 + x)
      (attr.hierarchy/update-attr :y2 + y)))

(defmethod hierarchy/scale :line
  [el ratio pivot-point]
  (let [{:keys [x1 y1 x2 y2]} (:attrs el)
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        dimentions (bounds/->dimensions (hierarchy/bounds el))
        [x y] (mat/sub dimentions (mat/mul dimentions ratio))
        pivot-diff (mat/sub pivot-point dimentions)
        offset (mat/sub pivot-diff (mat/mul pivot-diff ratio))]
    (-> el
        (attr.hierarchy/update-attr (if (< x1 x2) :x1 :x2) + x)
        (attr.hierarchy/update-attr (if (< y1 y2) :y1 :y2) + y)
        (hierarchy/translate offset))))

(defmethod hierarchy/path :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    (str/join " " ["M" x1 y1
                   "L" x2 y2])))

(defmethod hierarchy/render-edit :line
  [el]
  (let [offset (element/offset el)
        {{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        [x1 y1] (mat/add offset [x1 y1])
        [x2 y2] (mat/add offset [x2 y2])]
    [:g
     {:key ::edit-handles}
     (map (fn [handle]
            ^{:key (:id handle)}
            [handle.v/square handle
             [:title {:key (str (:id handle) "-title")} (name (:id handle))]])
          [{:x x1
            :y y1
            :id :starting-point
            :type :handle
            :action :edit
            :element (:id el)}
           {:x x2
            :y y2
            :id :ending-point
            :type :handle
            :action :edit
            :element (:id el)}])]))

(defmethod hierarchy/edit :line
  [el [x y] handle]
  (case handle
    :starting-point
    (-> el
        (attr.hierarchy/update-attr :x1 + x)
        (attr.hierarchy/update-attr :y1 + y))

    :ending-point
    (-> el
        (attr.hierarchy/update-attr :x2 + x)
        (attr.hierarchy/update-attr :y2 + y))
    el))

(defmethod hierarchy/bounds :line
  [el]
  (let [{{:keys [x1 y1 x2 y2]} :attrs} el
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))

#_(defmethod hierarchy/snapping-points :line
    [el]
    (let [{{:keys [x1 y1 x2 y2]} :attrs} el
          [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
      [(with-meta [x1 y1] {:label "line start"})
       (with-meta [x2 y2] {:label "line end"})]))
