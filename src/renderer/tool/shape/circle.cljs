(ns renderer.tool.shape.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive :circle ::tool/shape)

(defmethod tool/properties :circle
  []
  {:icon "circle-alt"
   :description "The <circle> SVG element is an SVG basic shape, used to draw
                 circles based on a center point and a radius."
   :locked-ratio? true
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod tool/drag :circle
  [{:keys [adjusted-pointer-offset active-document adjusted-pointer-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        radius (mat/distance adjusted-pointer-pos adjusted-pointer-offset)
        attrs {:cx offset-x
               :cy offset-y
               :fill fill
               :stroke stroke
               :r radius}]
    (element.h/set-temp db {:type :element :tag :circle :attrs attrs})))

(defmethod tool/translate :circle
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :cx + x)
      (attr.hierarchy/update-attr :cy + y)))

(defmethod tool/scale :circle
  [el ratio pivot-point]
  (let [dimentions (bounds/->dimensions (tool/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :r * ratio)
        (tool/translate offset))))

(defmethod tool/bounds :circle
  [{{:keys [cx cy r]} :attrs}]
  (let [[cx cy r] (map units/unit->px [cx cy r])]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod tool/area :circle
  [{{:keys [r]} :attrs}]
  (* Math/PI (Math/pow (units/unit->px r) 2)))

(defmethod tool/path :circle
  [{{:keys [cx cy r]} :attrs}]
  (let [[cx cy r] (map units/unit->px [cx cy r])]
    (str/join " " ["M" (+ cx r) cy
                   "A" r r 0 0 1 cx (+ cy r)
                   "A" r r 0 0 1 (- cx r) cy
                   "A" r r 0 0 1 (+ cx r) cy
                   "z"])))

(defmethod tool/edit :circle
  [el [x _y] handle]
  (case handle
    :r (attr.hierarchy/update-attr el :r #(abs (+ % x)))
    el))

(defmethod tool/render-edit :circle
  [{:keys [id] :as el}]
  (let [bounds (:bounds el)
        [cx cy] (bounds/center bounds)
        r (/ (first (bounds/->dimensions bounds)) 2)]
    [:g
     [overlay/line cx cy (+ cx r) cy]
     [overlay/label (str (units/->fixed r)) [(+ cx (/ r 2)) cy]]
     [overlay/times cx cy]
     [overlay/square-handle {:x (+ cx r)
                             :y cy
                             :id :r
                             :type :handle
                             :tag :edit
                             :cursor "move"
                             :element id}
      [:title {:key "r-title"} "r"]]]))
