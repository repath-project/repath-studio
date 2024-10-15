(ns renderer.element.impl.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [clojure.core.matrix :as mat]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive ::hierarchy/box ::hierarchy/renderable)

(defmethod hierarchy/translate ::hierarchy/box
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :x + x)
      (attr.hierarchy/update-attr :y + y)))

(defmethod hierarchy/scale ::hierarchy/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (attr.hierarchy/update-attr :width * x)
        (attr.hierarchy/update-attr :height * y)
        (hierarchy/translate offset))))

(defmethod hierarchy/edit ::hierarchy/box
  [el [x y] handle]
  (case handle
    :position
    (-> el
        (attr.hierarchy/update-attr :width #(max 0 (- % x)))
        (attr.hierarchy/update-attr :height #(max 0 (- % y)))
        (hierarchy/translate [x y]))

    :size
    (-> el
        (attr.hierarchy/update-attr :width #(max 0 (+ % x)))
        (attr.hierarchy/update-attr :height #(max 0 (+ % y))))

    el))

(defmethod hierarchy/render-edit ::hierarchy/box
  [el]
  (let [el-bounds (:bounds el)
        [x y] el-bounds
        [width height] (bounds/->dimensions el-bounds)]
    [:g
     (for [handle [{:x x :y y :id :position}
                   {:x (+ x width) :y (+ y height) :id :size}]]
       (let [handle (merge handle {:type :handle
                                   :action :edit
                                   :cursor "move"
                                   :element (:id el)})]
         ^{:key (:id handle)}
         [handle.v/square handle
          ^{:key (str (:id handle) "-title")}
          [:title (name (:id handle))]]))]))

(defmethod hierarchy/bounds ::hierarchy/box
  [el]
  (let [{{:keys [x y width height]} :attrs} el
        [x y width height] (mapv units/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod hierarchy/area ::hierarchy/box
  [el]
  (let [{{:keys [width height]} :attrs} el]
    (apply * (map units/unit->px [width height]))))
