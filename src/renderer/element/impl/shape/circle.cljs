(ns renderer.element.impl.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.units :as units]))

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
  (element/update-attrs-with el + [[:cx x]
                                   [:cy y]]))

(defmethod hierarchy/scale :circle
  [el ratio pivot-point]
  (let [dimentions (bounds/->dimensions (hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :r * ratio)
        (hierarchy/translate offset))))

(defmethod hierarchy/bounds :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map units/unit->px [cx cy r])]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod hierarchy/area :circle
  [el]
  (-> (get-in el [:attrs :r])
      (units/unit->px)
      (Math/pow 2)
      (* Math/PI)))

(defmethod hierarchy/path :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map units/unit->px [cx cy r])]
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
  (let [bounds (:bounds el)
        [cx cy] (bounds/center bounds)
        r (/ (first (bounds/->dimensions bounds)) 2)]
    [:g
     [overlay/line cx cy (+ cx r) cy]
     [overlay/label (str (.toFixed r 2)) [(+ cx (/ r 2)) cy]]
     [overlay/times cx cy]
     [handle.v/square {:x (+ cx r)
                       :y cy
                       :id :r
                       :type :handle
                       :action :edit
                       :cursor "move"
                       :element (:id el)}
      [:title {:key "r-title"} "r"]]]))
