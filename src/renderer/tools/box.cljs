(ns renderer.tools.box
  "This serves as an abstraction for box elements that share the
   :x :y :width :height attributes (e.g. rect, svg, image)."
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.units :as units]
   [renderer.utils.bounds :as bounds]))

(derive ::tools/box ::tools/element)

(defmethod tools/translate ::tools/box
  [el [x y]]
  (-> el
      (hierarchy/update-attr :x + x)
      (hierarchy/update-attr :y + y)))


(defmethod tools/scale ::tools/box
  [el ratio pivot-point]
  (let [[x y] ratio
        offset (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (-> el
        (hierarchy/update-attr :width * x)
        (hierarchy/update-attr :height * y)
        (tools/translate offset))))

(defmethod tools/edit ::tools/box
  [el [x y] handler]
  (case (keyword (name handler))
    :position
    (-> el
        (hierarchy/update-attr :width #(max 0 (- % x)))
        (hierarchy/update-attr :height #(max 0 (- % y)))
        (tools/translate [x y]))

    :size
    (-> el
        (hierarchy/update-attr :width #(max 0 (+ % x)))
        (hierarchy/update-attr :height #(max 0 (+ % y))))

    el))

(defmethod tools/render-edit ::tools/box
  [el]
  (let [el-bounds @(rf/subscribe [:element/el-bounds el])
        [x y] el-bounds
        [width height] (bounds/->dimensions el-bounds)]
    [:g {:key ::edit-handles}
     (map (fn [handle]
            (let [handler (merge handle {:type :handler
                                         :tag :edit
                                         :key (keyword (:key el) (:key handle))
                                         :element (:key el)})]
              [overlay/square-handle handler
               ^{:key (:key handler)} [:title (name (:key handler))]]))
          [{:x x :y y :key (keyword (:key el) :position)}
           {:x (+ x width) :y (+ y height) :key (keyword (:key el) :size)}])]))

(defmethod tools/bounds ::tools/box
  [{{:keys [x y width height]} :attrs}]
  (let [[x y width height] (mapv units/unit->px [x y width height])]
    [x y (+ x width) (+ y height)]))

(defmethod tools/area ::tools/box
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))
