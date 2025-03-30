(ns renderer.element.impl.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.tool.views :as tool.v]
   [renderer.utils.attribute :as attr]
   [renderer.utils.element :as element]
   [renderer.utils.length :as length]))

(derive ::hierarchy/polyshape ::hierarchy/shape)

(def partition-to-px
  (comp (map length/unit->px)
        (partition-all 2)))

(defn points->px
  [points]
  (into [] partition-to-px (attr/str->seq points)))

(defn translate
  [[offset-x offset-y] points [point-x point-y]]
  (conj points
        (when point-x (length/transform point-x + offset-x))
        (when point-y (length/transform point-y + offset-y))))

(defmethod hierarchy/translate ::hierarchy/polyshape
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (attr/str->seq %)
                   (transduce (partition-all 2) (partial translate offset) [])
                   (str/join " ")
                   (str/trim))))

(defmethod hierarchy/scale ::hierarchy/polyshape
  [el ratio pivot-point]
  (let [bounds-min (take 2 (hierarchy/bbox el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> (attr/str->seq %)
                     (transduce
                      partition-to-px
                      (fn [points point]
                        (let [rel-point (mat/sub bounds-min point)
                              offset (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                          (translate offset points point))) [])
                     (str/join " ")
                     (str/trim)))))

(defmethod hierarchy/render-edit ::hierarchy/polyshape
  [el]
  [:g (map-indexed (fn [index [x y]]
                     (let [[x y] (mapv length/unit->px [x y])
                           [x y] (mat/add (element/offset el) [x y])]
                       ^{:key index}
                       [tool.v/square-handle {:id (keyword (str index))
                                              :x x
                                              :y y
                                              :label "point"
                                              :type :handle
                                              :cursor "move"
                                              :action :edit
                                              :element (:id el)}]))
                   (attr/points->vec (-> el :attrs :points)))])

(defmethod hierarchy/edit ::hierarchy/polyshape
  [el [x y] handle]
  (let [index (js/parseInt (name handle))]
    (update-in el [:attrs :points] #(-> (attr/points->vec %)
                                        (update index (fn [[px py]]
                                                        (list (length/transform px + x)
                                                              (length/transform py + y))))
                                        (flatten)
                                        (->> (str/join " ")
                                             (str/trim))))))

(defmethod hierarchy/bbox ::hierarchy/polyshape
  [el]
  (let [points (-> el :attrs :points attr/points->vec)
        min-x (apply min (map #(length/unit->px (first %)) points))
        min-y (apply min (map #(length/unit->px (second %)) points))
        max-x (apply max (map #(length/unit->px (first %)) points))
        max-y (apply max (map #(length/unit->px (second %)) points))]
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

(defmethod hierarchy/area ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (points->px points)]
    (calc-polygon-area points-v)))

(defmethod hierarchy/centroid ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (points->px points)]
    (mat/div (reduce mat/add [0 0] points-v)
             (count points-v))))

(defmethod hierarchy/snapping-points ::hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (points->px points))
