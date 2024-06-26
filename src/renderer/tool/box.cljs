(ns renderer.tool.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [clojure.core.matrix :as mat]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.units :as units]
   [renderer.utils.bounds :as bounds]))

(derive ::tool/box ::tool/renderable)

(defmethod tool/translate ::tool/box
  [el [x y]]
  (-> el
      (hierarchy/update-attr :x + x)
      (hierarchy/update-attr :y + y)))


(defmethod tool/scale ::tool/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (hierarchy/update-attr :width * x)
        (hierarchy/update-attr :height * y)
        (tool/translate offset))))

(defmethod tool/edit ::tool/box
  [el [x y] handle]
  (case (keyword (name handle))
    :position
    (-> el
        (hierarchy/update-attr :width #(max 0 (- % x)))
        (hierarchy/update-attr :height #(max 0 (- % y)))
        (tool/translate [x y]))

    :size
    (-> el
        (hierarchy/update-attr :width #(max 0 (+ % x)))
        (hierarchy/update-attr :height #(max 0 (+ % y))))

    el))

(defmethod tool/render-edit ::tool/box
  [el]
  (let [el-bounds (:bounds el)
        [x y] el-bounds
        [width height] (bounds/->dimensions el-bounds)]
    [:g {:key ::edit-handles}
     (map (fn [handle]
            (let [handle (merge handle {:type :handle
                                        :tag :edit
                                        :key (keyword (:key el) (:key handle))
                                        :element (:key el)})]
              [overlay/square-handle handle
               ^{:key (:key handle)} [:title (name (:key handle))]]))
          [{:x x :y y :key (keyword (:key el) :position)}
           {:x (+ x width) :y (+ y height) :key (keyword (:key el) :size)}])]))

(defmethod tool/bounds ::tool/box
  [{{:keys [x y width height]} :attrs}]
  (let [[x y width height] (mapv units/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod tool/area ::tool/box
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))
