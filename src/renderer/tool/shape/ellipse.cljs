(ns renderer.tool.shape.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :ellipse ::tool/shape)

(defmethod tool/properties :ellipse
  []
  {:icon "ellipse-alt"
   :description "The <ellipse> element is an SVG basic shape, used to create 
                 ellipses based on a center coordinate, and both their x and 
                 y radius."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod tool/drag :ellipse
  [{:keys [adjusted-pointer-offset active-document adjusted-pointer-pos] :as db} e]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        lock-ratio? (pointer/ctrl? e)
        rx (abs (- pos-x offset-x))
        ry (abs (- pos-y offset-y))
        attrs {:cx offset-x
               :cy offset-y
               :fill fill
               :stroke stroke
               :rx (if lock-ratio? (min rx ry) rx)
               :ry (if lock-ratio? (min rx ry) ry)}]
    (element.h/set-temp db {:type :element
                            :tag :ellipse
                            :attrs attrs})))

(defmethod tool/translate :ellipse
  [el [x y]]
  (-> el
      (hierarchy/update-attr :cx + x)
      (hierarchy/update-attr :cy + y)))

(defmethod tool/scale :ellipse
  [el ratio pivot-point]
  (let [[x y] ratio
        dimentions (bounds/->dimensions (tool/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (hierarchy/update-attr :rx * x)
        (hierarchy/update-attr :ry * y)
        (tool/translate offset))))

(defmethod tool/bounds :ellipse
  [{{:keys [cx cy rx ry]} :attrs}]
  (let [[cx cy rx ry] (map units/unit->px [cx cy rx ry])]
    [(- cx rx) (- cy ry) (+ cx rx) (+ cy ry)]))

(defmethod tool/path :ellipse
  [{{:keys [cx cy rx ry]} :attrs}]
  (let [[cx cy rx ry] (mapv units/unit->px [cx cy rx ry])]
    (str/join " " ["M" (+ cx rx) cy
                   "A" rx ry 0 0 1 cx (+ cy ry)
                   "A" rx ry 0 0 1 (- cx rx) cy
                   "A" rx ry 0 0 1 (+ cx rx) cy
                   "z"])))

(defmethod tool/edit :ellipse
  [el [x y] handle]
  (case (keyword (name handle))
    :rx (hierarchy/update-attr el :rx #(abs (+ % x)))
    :ry (hierarchy/update-attr el :ry #(abs (- % y)))
    el))

(defmethod tool/render-edit :ellipse
  [{:keys [key] :as el}]
  (let [bounds (:bounds el)
        [cx cy] (bounds/center bounds)
        [rx ry] (mat/div (bounds/->dimensions bounds) 2)]
    [:g ::edit-handles
     [overlay/times cx cy]
     [overlay/line cx cy (+ cx rx) cy]
     [overlay/label (str (units/->fixed rx)) [(+ cx (/ rx 2)) cy]]
     [overlay/line cx cy cx (- cy ry)]
     [overlay/label (str (units/->fixed ry)) [cx (- cy (/ ry 2))]]
     (map (fn [handle]
            [overlay/square-handle (merge handle
                                          {:type :handle
                                           :tag :edit
                                           :element key})
             ^{:key (:key handle)}
             [:title (name (:key handle))]])
          [{:x (+ cx rx) :y cy :key (keyword (:key el) :rx)}
           {:x cx :y (- cy ry) :key (keyword (:key el) :ry)}])]))
