(ns renderer.element.impl.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.utils.attribute :as utils.attr]
   [renderer.utils.element :as element]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.units :as units]))

(derive ::hierarchy/polyshape ::hierarchy/shape)

(defn translate
  [[offset-x offset-y] points [point-x point-y]]
  (conj points
        (when point-x (units/transform point-x + offset-x))
        (when point-y (units/transform point-y + offset-y))))

(defmethod hierarchy/translate ::hierarchy/polyshape
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (utils.attr/str->seq %)
                   (transduce (partition-all 2) (partial translate offset) [])
                   (str/join " "))))

(defmethod hierarchy/scale ::hierarchy/polyshape
  [el ratio pivot-point]
  (let [bounds-start (take 2 (hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> (utils.attr/str->seq %)
                     (transduce
                      utils.attr/partition-to-px
                      (fn [points point]
                        (let [rel-point (mat/sub (take 2 bounds-start) point)
                              offset (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                          (translate offset points point))) [])
                     (str/join " ")))))

(defmethod hierarchy/render-edit ::hierarchy/polyshape
  [el]
  [:g (map-indexed (fn [index [x y]]
                     (let [[x y] (mapv units/unit->px [x y])
                           [x y] (mat/add (element/offset el) [x y])]
                       ^{:key index}
                       [overlay/square-handle {:id (keyword (str index))
                                               :x x
                                               :y y
                                               :type :handle
                                               :cursor "move"
                                               :tag :edit
                                               :element (:id el)}]))
                   (utils.attr/points->vec (-> el :attrs :points)))])

(defmethod hierarchy/edit ::hierarchy/polyshape
  [el [x y] handle]
  (let [index (js/parseInt (name handle))]
    (update-in el [:attrs :points] #(-> (utils.attr/points->vec %)
                                        (update index (fn [[px py]]
                                                        (list (units/transform px + x)
                                                              (units/transform py + y))))
                                        (flatten)
                                        (->> (str/join " "))))))

(defmethod hierarchy/bounds ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->vec points)
        x1 (apply min (map #(units/unit->px (first %)) points-v))
        y1 (apply min (map #(units/unit->px (second %)) points-v))
        x2 (apply max (map #(units/unit->px (first %)) points-v))
        y2 (apply max (map #(units/unit->px (second %)) points-v))]
    [x1 y1 x2 y2]))

(defn calc-polygon-area
  [vertices]
  (let [count-v (count vertices)]
    (/ (reduce-kv (fn [area index point]
                    (let [point-b (if (= index (dec count-v))
                                    (first vertices)
                                    (nth vertices (inc index)))]
                      (+ area
                         (- (* (first point) (second point-b))
                            (* (first point-b) (second point))))))
                  0
                  vertices) 2)))

(defmethod hierarchy/area ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->px points)]
    (calc-polygon-area points-v)))

(defmethod hierarchy/centroid ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->px points)]
    (mat/div (reduce mat/add [0 0] points-v)
             (count points-v))))

(defmethod hierarchy/snapping-points ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (utils.attr/points->px points))
