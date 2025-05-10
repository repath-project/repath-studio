(ns renderer.element.impl.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.views :as tool.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]))

(derive ::element.hierarchy/polyshape ::element.hierarchy/shape)

(def partition-to-px
  (comp (map utils.length/unit->px)
        (partition-all 2)))

(defn points->px
  [points]
  (into [] partition-to-px (utils.attribute/str->seq points)))

(defn translate
  [[offset-x offset-y] points [point-x point-y]]
  (conj points
        (when point-x (utils.length/transform point-x + offset-x))
        (when point-y (utils.length/transform point-y + offset-y))))

(defmethod element.hierarchy/translate ::element.hierarchy/polyshape
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (utils.attribute/str->seq %)
                   (transduce (partition-all 2) (partial translate offset) [])
                   (string/join " ")
                   (string/trim))))

(defmethod element.hierarchy/scale ::element.hierarchy/polyshape
  [el ratio pivot-point]
  (let [bounds-min (take 2 (element.hierarchy/bbox el))
        pivot-point (matrix/sub pivot-point (matrix/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> (utils.attribute/str->seq %)
                     (transduce
                      partition-to-px
                      (fn [points point]
                        (let [rel-point (matrix/sub bounds-min point)
                              offset (matrix/add pivot-point (matrix/sub rel-point (matrix/mul rel-point ratio)))]
                          (translate offset points point))) [])
                     (string/join " ")
                     (string/trim)))))

(defmethod element.hierarchy/render-edit ::element.hierarchy/polyshape
  [el]
  [:g (map-indexed (fn [index [x y]]
                     (let [[x y] (mapv utils.length/unit->px [x y])
                           [x y] (matrix/add (utils.element/offset el) [x y])]
                       ^{:key index}
                       [tool.views/square-handle {:id (keyword (str index))
                                                  :x x
                                                  :y y
                                                  :label "point"
                                                  :type :handle
                                                  :cursor "move"
                                                  :action :edit
                                                  :element (:id el)}]))
                   (utils.attribute/points->vec (-> el :attrs :points)))])

(defmethod element.hierarchy/edit ::element.hierarchy/polyshape
  [el [x y] handle]
  (let [index (js/parseInt (name handle))]
    (update-in el [:attrs :points] #(-> (utils.attribute/points->vec %)
                                        (update index (fn [[px py]]
                                                        (list (utils.length/transform px + x)
                                                              (utils.length/transform py + y))))
                                        (flatten)
                                        (->> (string/join " ")
                                             (string/trim))))))

(defmethod element.hierarchy/bbox ::element.hierarchy/polyshape
  [el]
  (let [points (-> el :attrs :points utils.attribute/points->vec)
        min-x (apply min (map #(utils.length/unit->px (first %)) points))
        min-y (apply min (map #(utils.length/unit->px (second %)) points))
        max-x (apply max (map #(utils.length/unit->px (first %)) points))
        max-y (apply max (map #(utils.length/unit->px (second %)) points))]
    [min-x min-y max-x max-y]))

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

(defmethod element.hierarchy/area ::element.hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (points->px points)]
    (calc-polygon-area points-v)))

(defmethod element.hierarchy/centroid ::element.hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (points->px points)]
    (matrix/div (reduce matrix/add [0 0] points-v)
                (count points-v))))

(defmethod element.hierarchy/snapping-points ::element.hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (points->px points))
