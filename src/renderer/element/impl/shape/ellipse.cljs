(ns renderer.element.impl.shape.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.units :as units]))

(derive :ellipse ::hierarchy/shape)

(defmethod hierarchy/properties :ellipse
  []
  {:icon "ellipse"
   :description "The <ellipse> element is an SVG basic shape, used to create
                 ellipses based on a center coordinate, and both their x and
                 y radius."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod hierarchy/translate :ellipse
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :cx + x)
      (attr.hierarchy/update-attr :cy + y)))

(defmethod hierarchy/scale :ellipse
  [el ratio pivot-point]
  (let [[x y] ratio
        dimentions (bounds/->dimensions (hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (attr.hierarchy/update-attr :rx * x)
        (attr.hierarchy/update-attr :ry * y)
        (hierarchy/translate offset))))

(defmethod hierarchy/bounds :ellipse
  [el]
  (let [{{:keys [cx cy rx ry]} :attrs} el
        [cx cy rx ry] (map units/unit->px [cx cy rx ry])]
    [(- cx rx) (- cy ry) (+ cx rx) (+ cy ry)]))

(defmethod hierarchy/path :ellipse
  [el]
  (let [{{:keys [cx cy rx ry]} :attrs} el
        [cx cy rx ry] (mapv units/unit->px [cx cy rx ry])]
    (str/join " " ["M" (+ cx rx) cy
                   "A" rx ry 0 0 1 cx (+ cy ry)
                   "A" rx ry 0 0 1 (- cx rx) cy
                   "A" rx ry 0 0 1 (+ cx rx) cy
                   "z"])))

(defmethod hierarchy/edit :ellipse
  [el [x y] handle]
  (case handle
    :rx (attr.hierarchy/update-attr el :rx #(abs (+ % x)))
    :ry (attr.hierarchy/update-attr el :ry #(abs (- % y)))
    el))

(defmethod hierarchy/render-edit :ellipse
  [el]
  (let [bounds (:bounds el)
        [cx cy] (bounds/center bounds)
        [rx ry] (mat/div (bounds/->dimensions bounds) 2)]
    [:g ::edit-handles
     [overlay/times cx cy]
     [overlay/line cx cy (+ cx rx) cy]
     [overlay/label (str (.toFixed rx 2)) [(+ cx (/ rx 2)) cy]]
     [overlay/line cx cy cx (- cy ry)]
     [overlay/label (str (.toFixed ry 2)) [cx (- cy (/ ry 2))]]
     (map (fn [handle]
            ^{:key (:id handle)}
            [handle.v/square
             (merge handle {:type :handle
                            :action :edit
                            :cursor "move"
                            :element (:id el)})
             [:title
              {:key (str (:id handle) "-title")}
              (name (:id handle))]])
          [{:x (+ cx rx) :y cy :id :rx}
           {:x cx :y (- cy ry) :id :ry}])]))
