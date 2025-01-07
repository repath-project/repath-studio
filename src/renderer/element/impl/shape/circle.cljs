(ns renderer.element.impl.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
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

(derive :circle ::hierarchy/shape)

(defmethod hierarchy/properties :circle
  []
  {:icon "circle"
   :description "The <circle> SVG element is an SVG basic shape, used to draw
                 circles based on a center point and a radius."
   :ratio-locked true
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod hierarchy/translate :circle
  [el [x y]]
  (element/update-attrs-with el + [[:cx x] [:cy y]]))

(defmethod hierarchy/scale :circle
  [el ratio pivot-point]
  (let [dimensions (bounds/->dimensions (hierarchy/bbox el))
        pivot-point (mat/sub pivot-point (mat/div dimensions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :r * ratio)
        (hierarchy/translate offset))))

(defmethod hierarchy/bbox :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map length/unit->px [cx cy r])]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod hierarchy/area :circle
  [el]
  (-> (get-in el [:attrs :r])
      (length/unit->px)
      (Math/pow 2)
      (* Math/PI)))

(defmethod hierarchy/path :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map length/unit->px [cx cy r])]
    (str/join " " ["M" (+ cx r) cy
                   "A" r r 0 0 1 cx (+ cy r)
                   "A" r r 0 0 1 (- cx r) cy
                   "A" r r 0 0 1 (+ cx r) cy
                   "z"])))

(defmethod hierarchy/edit :circle
  [el [x _y] handle]
  (case handle
    :r (attr.hierarchy/update-attr el :r #(abs (+ % x)))
    el))

(defmethod hierarchy/render-edit :circle
  [el]
  (let [bbox (:bbox el)
        [cx cy] (bounds/center bbox)
        r (/ (first (bounds/->dimensions bbox)) 2)]
    [:g
     [svg/line [cx cy] [(+ cx r) cy]]
     [svg/label (str (.toFixed r 2)) [(+ cx (/ r 2)) cy]]
     [svg/times [cx cy]]
     [tool.v/square-handle {:x (+ cx r)
                            :y cy
                            :id :r
                            :type :handle
                            :action :edit
                            :cursor "move"
                            :element (:id el)}
      [:title {:key "r-title"} "r"]]]))
