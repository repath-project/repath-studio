(ns renderer.element.impl.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.element.hierarchy :as hierarchy]
   [renderer.handle.views :as handle.v]
   [renderer.utils.attribute :as attr]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive ::hierarchy/polyshape ::hierarchy/shape)

(def partition-to-px
  (comp
   (map units/unit->px)
   (partition-all 2)))

(defn points->px
  [points]
  (into [] partition-to-px (attr/str->seq points)))

(defn translate
  [[offset-x offset-y] points [point-x point-y]]
  (conj points
        (when point-x (units/transform point-x + offset-x))
        (when point-y (units/transform point-y + offset-y))))

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
  (let [bounds-start (take 2 (hierarchy/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> (attr/str->seq %)
                     (transduce
                      partition-to-px
                      (fn [points point]
                        (let [rel-point (mat/sub bounds-start point)
                              offset (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                          (translate offset points point))) [])
                     (str/join " ")
                     (str/trim)))))

(defmethod hierarchy/render-edit ::hierarchy/polyshape
  [el]
  [:g (map-indexed (fn [index [x y]]
                     (let [[x y] (mapv units/unit->px [x y])
                           [x y] (mat/add (element/offset el) [x y])]
                       ^{:key index}
                       [handle.v/square {:id (keyword (str index))
                                         :x x
                                         :y y
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
                                                        (list (units/transform px + x)
                                                              (units/transform py + y))))
                                        (flatten)
                                        (->> (str/join " ")
                                             (str/trim))))))

(defmethod hierarchy/bounds ::hierarchy/polyshape
  [el]
  (let [points (-> el :attrs :points attr/points->vec)
        x1 (apply min (map #(units/unit->px (first %)) points))
        y1 (apply min (map #(units/unit->px (second %)) points))
        x2 (apply max (map #(units/unit->px (first %)) points))
        y2 (apply max (map #(units/unit->px (second %)) points))]
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
