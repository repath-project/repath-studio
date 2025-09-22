(ns renderer.element.impl.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.views :as tool.views]
   [renderer.utils.bounds :as utils.bounds]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(derive ::element.hierarchy/box ::element.hierarchy/renderable)

(defmethod element.hierarchy/translate ::element.hierarchy/box
  [el [x y]]
  (-> el
      (attribute.hierarchy/update-attr :x + x)
      (attribute.hierarchy/update-attr :y + y)))

(defmethod element.hierarchy/scale ::element.hierarchy/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (utils.element/scale-offset ratio pivot-point)]
    (-> el
        (attribute.hierarchy/update-attr :width * x)
        (attribute.hierarchy/update-attr :height * y)
        (element.hierarchy/translate offset))))

(defmethod element.hierarchy/edit ::element.hierarchy/box
  [el [x y] handle]
  (let [clamp (partial max 0)]
    (case handle
      :position
      (-> el
          (attribute.hierarchy/update-attr :width (comp clamp -) x)
          (attribute.hierarchy/update-attr :height (comp clamp -) y)
          (element.hierarchy/translate [x y]))

      :size
      (-> el
          (attribute.hierarchy/update-attr :width (comp clamp +) x)
          (attribute.hierarchy/update-attr :height (comp clamp +) y))

      el)))

(defmethod element.hierarchy/render-edit ::element.hierarchy/box
  [el]
  (let [el-bbox (:bbox el)
        [min-x min-y] el-bbox
        [w h] (utils.bounds/->dimensions el-bbox)]
    [:g
     (for [handle [{:x min-x
                    :y min-y
                    :id :position}
                   {:x (+ min-x w)
                    :y (+ min-y h)
                    :id :size}]]
       (let [handle (merge handle {:type :handle
                                   :action :edit
                                   :element-id (:id el)})]
         ^{:key (:id handle)}
         [tool.views/square-handle handle
          ^{:key (str (:id handle) "-title")}
          [:title (name (:id handle))]]))]))

(defmethod element.hierarchy/bbox ::element.hierarchy/box
  [el]
  (let [{{:keys [x y width height]} :attrs} el
        [x y width height] (mapv utils.length/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod element.hierarchy/area ::element.hierarchy/box
  [el]
  (let [{{:keys [width height]} :attrs} el]
    (apply * (map utils.length/unit->px [width height]))))

#_(defmethod hierarchy/snapping-points ::element.hierarchy/box
    [el]
    (let [{{:keys [x y width height]} :attrs} el
          [x y w h] (mapv utils.length/unit->px [x y width height])]
      [(with-meta [x y] {:label "box corner"})
       (with-meta [(+ x w) y] {:label "box corner"})
       (with-meta [(+ x w) (+ y h)] {:label "box corner"})
       (with-meta [x (+ y h)] {:label "box corner"})]))
