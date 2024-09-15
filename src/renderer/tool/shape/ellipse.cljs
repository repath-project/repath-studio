(ns renderer.tool.shape.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :ellipse ::tool.hierarchy/shape)

(defmethod tool.hierarchy/properties :ellipse
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

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defmethod tool.hierarchy/drag :ellipse
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

(defmethod tool.hierarchy/translate :ellipse
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :cx + x)
      (attr.hierarchy/update-attr :cy + y)))

(defmethod tool.hierarchy/scale :ellipse
  [el ratio pivot-point]
  (let [[x y] ratio
        dimentions (bounds/->dimensions (tool.hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/div dimentions 2))
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (attr.hierarchy/update-attr :rx * x)
        (attr.hierarchy/update-attr :ry * y)
        (tool.hierarchy/translate offset))))

(defmethod tool.hierarchy/bounds :ellipse
  [{{:keys [cx cy rx ry]} :attrs}]
  (let [[cx cy rx ry] (map units/unit->px [cx cy rx ry])]
    [(- cx rx) (- cy ry) (+ cx rx) (+ cy ry)]))

(defmethod tool.hierarchy/path :ellipse
  [{{:keys [cx cy rx ry]} :attrs}]
  (let [[cx cy rx ry] (mapv units/unit->px [cx cy rx ry])]
    (str/join " " ["M" (+ cx rx) cy
                   "A" rx ry 0 0 1 cx (+ cy ry)
                   "A" rx ry 0 0 1 (- cx rx) cy
                   "A" rx ry 0 0 1 (+ cx rx) cy
                   "z"])))

(defmethod tool.hierarchy/edit :ellipse
  [el [x y] handle]
  (case handle
    :rx (attr.hierarchy/update-attr el :rx #(abs (+ % x)))
    :ry (attr.hierarchy/update-attr el :ry #(abs (- % y)))
    el))

(defmethod tool.hierarchy/render-edit :ellipse
  [{:keys [id] :as el}]
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
            [overlay/square-handle
             (merge handle {:type :handle
                            :tag :edit
                            :cursor "move"
                            :element id})
             [:title
              {:key (str (:id handle) "-title")}
              (name (:id handle))]])
          [{:x (+ cx rx) :y cy :id :rx}
           {:x cx :y (- cy ry) :id :ry}])]))
