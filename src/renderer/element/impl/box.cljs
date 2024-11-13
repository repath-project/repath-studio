(ns renderer.element.impl.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive ::hierarchy/box ::hierarchy/renderable)

(defmethod hierarchy/translate ::hierarchy/box
  [el [x y]]
  (element/update-attrs-with el + [[:x x] [:y y]]))

(defmethod hierarchy/scale ::hierarchy/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> (element/update-attrs-with el * [[:width x] [:height y]])
        (hierarchy/translate offset))))

(defmethod hierarchy/edit ::hierarchy/box
  [el [x y] handle]
  (case handle
    :position
    (-> (element/update-attrs-with el (comp (partial max 0) -) [[:width x] [:height y]])
        (hierarchy/translate [x y]))

    :size
    (element/update-attrs-with el (comp (partial max 0) +) [[:width x] [:height y]])

    el))

(defmethod hierarchy/render-edit ::hierarchy/box
  [el]
  (let [el-bounds (:bounds el)
        [x y] el-bounds
        [width height] (bounds/->dimensions el-bounds)]
    [:g
     (for [handle [{:x x
                    :y y
                    :id :position}
                   {:x (+ x width)
                    :y (+ y height)
                    :id :size}]]
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

#_(defmethod hierarchy/snapping-points ::hierarchy/box
    [el]
    (let [{{:keys [x y width height]} :attrs} el
          [x y width height] (mapv units/unit->px [x y width height])]
      [(with-meta [x y] {:label "box corner"})
       (with-meta [(+ x width) y]  {:label "box corner"})
       (with-meta [(+ x width) (+ y height)] {:label "box corner"})
       (with-meta [x (+ y height)] {:label "box corner"})]))
