(ns renderer.tool.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive :circle ::tool.hierarchy/shape)

(defmethod tool.hierarchy/properties :circle
  []
  {:icon "circle-alt"
   :description "The <circle> SVG element is an SVG basic shape, used to draw
                 circles based on a center point and a radius."
   :ratio-locked true
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod tool.hierarchy/drag :circle
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

(defmethod tool.hierarchy/translate :circle
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :cx + x)
      (attr.hierarchy/update-attr :cy + y)))

(defmethod tool.hierarchy/scale :circle
  [el ratio pivot-point]
  (let [dimentions (bounds/->dimensions (tool.hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :r * ratio)
        (tool.hierarchy/translate offset))))

(defmethod tool.hierarchy/bounds :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map units/unit->px [cx cy r])]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod tool.hierarchy/area :circle
  [el]
  (-> (get-in el [:attrs :r])
      (units/unit->px)
      (Math/pow 2)
      (* Math/PI)))

(defmethod tool.hierarchy/path :circle
  [el]
  (let [{{:keys [cx cy r]} :attrs} el
        [cx cy r] (map units/unit->px [cx cy r])]
    (str/join " " ["M" (+ cx r) cy
                   "A" r r 0 0 1 cx (+ cy r)
                   "A" r r 0 0 1 (- cx r) cy
                   "A" r r 0 0 1 (+ cx r) cy
                   "z"])))

(defmethod tool.hierarchy/edit :circle
  [el [x _y] handle]
  (case handle
    :r (attr.hierarchy/update-attr el :r #(abs (+ % x)))
    el))

(defmethod tool.hierarchy/render-edit :circle
  [el]
  (let [bounds (:bounds el)
        [cx cy] (bounds/center bounds)
        r (/ (first (bounds/->dimensions bounds)) 2)]
    [:g
     [overlay/line cx cy (+ cx r) cy]
     [overlay/label (str (.toFixed r 2)) [(+ cx (/ r 2)) cy]]
     [overlay/times cx cy]
     [overlay/square-handle {:x (+ cx r)
                             :y cy
                             :id :r
                             :type :handle
                             :tag :edit
                             :cursor "move"
                             :element (:id el)}
      [:title {:key "r-title"} "r"]]]))
