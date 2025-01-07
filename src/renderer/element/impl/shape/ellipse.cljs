(ns renderer.element.impl.shape.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.tool.views :as tool.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.length :as length]
   [renderer.utils.svg :as svg]))

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
  (element/update-attrs-with el + [[:cx x] [:cy y]]))

(defmethod hierarchy/scale :ellipse
  [el ratio pivot-point]
  (let [[x y] ratio
        dimensions (bounds/->dimensions (hierarchy/bbox el))
        pivot-point (mat/sub pivot-point (mat/div dimensions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> (element/update-attrs-with el * [[:rx x] [:ry y]])
        (hierarchy/translate offset))))

(defmethod hierarchy/bbox :ellipse
  [el]
  (let [{{:keys [cx cy rx ry]} :attrs} el
        [cx cy rx ry] (map length/unit->px [cx cy rx ry])]
    [(- cx rx) (- cy ry) (+ cx rx) (+ cy ry)]))

(defmethod hierarchy/path :ellipse
  [el]
  (let [{{:keys [cx cy rx ry]} :attrs} el
        [cx cy rx ry] (mapv length/unit->px [cx cy rx ry])]
    (str/join " " ["M" (+ cx rx) cy
                   "A" rx ry 0 0 1 cx (+ cy ry)
                   "A" rx ry 0 0 1 (- cx rx) cy
                   "A" rx ry 0 0 1 (+ cx rx) cy
                   "z"])))

(defmethod hierarchy/area :ellipse
  [el]
  (let [{{:keys [rx ry]} :attrs} el
        [rx ry] (map length/unit->px [rx ry])]
    (* Math/PI rx ry)))

(defmethod hierarchy/edit :ellipse
  [el [x y] handle]
  (case handle
    :rx (attr.hierarchy/update-attr el :rx #(abs (+ % x)))
    :ry (attr.hierarchy/update-attr el :ry #(abs (- % y)))
    el))

(defmethod hierarchy/render-edit :ellipse
  [el]
  (let [bbox (:bbox el)
        [cx cy] (bounds/center bbox)
        [rx ry] (mat/div (bounds/->dimensions bbox) 2)]
    [:g ::edit-handles
     [svg/times [cx cy]]
     [svg/line [cx cy] [(+ cx rx) cy]]
     [svg/label (str (.toFixed rx 2)) [(+ cx (/ rx 2)) cy]]
     [svg/line [cx cy] [cx (- cy ry)]]
     [svg/label (str (.toFixed ry 2)) [cx (- cy (/ ry 2))]]
     (map (fn [handle]
            ^{:key (:id handle)}
            [tool.v/square-handle
             (merge handle {:type :handle
                            :action :edit
                            :cursor "move"
                            :element (:id el)})
             [:title
              {:key (str (:id handle) "-title")}
              (name (:id handle))]])
          [{:x (+ cx rx) :y cy :id :rx}
           {:x cx :y (- cy ry) :id :ry}])]))
