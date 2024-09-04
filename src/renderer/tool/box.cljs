(ns renderer.tool.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [clojure.core.matrix :as mat]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive ::tool.hierarchy/box ::tool.hierarchy/renderable)

(defmethod tool.hierarchy/translate ::tool.hierarchy/box
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :x + x)
      (attr.hierarchy/update-attr :y + y)))


(defmethod tool.hierarchy/scale ::tool.hierarchy/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (attr.hierarchy/update-attr :width * x)
        (attr.hierarchy/update-attr :height * y)
        (tool.hierarchy/translate offset))))

(defmethod tool.hierarchy/edit ::tool.hierarchy/box
  [el [x y] handle]
  (case handle
    :position
    (-> el
        (attr.hierarchy/update-attr :width #(max 0 (- % x)))
        (attr.hierarchy/update-attr :height #(max 0 (- % y)))
        (tool.hierarchy/translate [x y]))

    :size
    (-> el
        (attr.hierarchy/update-attr :width #(max 0 (+ % x)))
        (attr.hierarchy/update-attr :height #(max 0 (+ % y))))

    el))

(defmethod tool.hierarchy/render-edit ::tool.hierarchy/box
  [el]
  (let [el-bounds (:bounds el)
        [x y] el-bounds
        [width height] (bounds/->dimensions el-bounds)]
    [:g
     (for [handle [{:x x :y y :id :position}
                   {:x (+ x width) :y (+ y height) :id :size}]]
       (let [handle (merge handle {:type :handle
                                   :tag :edit
                                   :cursor "move"
                                   :element (:id el)})]
         ^{:key (:id handle)}
         [overlay/square-handle handle
          ^{:key (str (:id handle) "-title")}
          [:title (name (:id handle))]]))]))

(defmethod tool.hierarchy/bounds ::tool.hierarchy/box
  [{{:keys [x y width height]} :attrs}]
  (let [[x y width height] (mapv units/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod tool.hierarchy/area ::tool.hierarchy/box
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))
