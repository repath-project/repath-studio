(ns renderer.element.impl.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [re-frame.core :as rf]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.tool.views :as tool.views]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.element :as utils.element]
   [renderer.utils.length :as utils.length]
   [renderer.utils.svg :as utils.svg]))

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
        offset (utils.element/scale-offset ratio pivot-point)]
    (update-in el
               [:attrs :points]
               #(->> (utils.attribute/str->seq %)
                     (transduce
                      partition-to-px
                      (fn [points point]
                        (let [rel-point (matrix/sub bounds-min point)
                              offset (->> ratio
                                          (matrix/mul rel-point)
                                          (matrix/sub rel-point)
                                          (matrix/add offset))]
                          (translate offset points point))) [])
                     (string/join " ")
                     (string/trim)))))

(defmethod element.hierarchy/render-edit ::element.hierarchy/polyshape
  [el]
  (let [clicked-element @(rf/subscribe [::app.subs/clicked-element])
        zoom @(rf/subscribe [::document.subs/zoom])
        margin (/ 15 zoom)]
    [:g (map-indexed (fn [index point]
                       (let [id (keyword (str index))
                             is-active (and (= (:id clicked-element) id)
                                            (= (:element clicked-element)
                                               (:id el)))
                             offset (utils.element/offset el)
                             [x y] (->> point
                                        (mapv utils.length/unit->px)
                                        (matrix/add offset))]
                         ^{:key index}
                         [:g
                          [tool.views/square-handle {:id (keyword (str index))
                                                     :x x
                                                     :y y
                                                     :label "point"
                                                     :type :handle
                                                     :action :edit
                                                     :element (:id el)}]
                          (when is-active
                            [utils.svg/label
                             (->> [x y]
                                  (mapv utils.length/->fixed)
                                  (string/join " "))
                             [(- x margin) (+ y margin)]
                             "end"])]))
                     (utils.attribute/points->vec (-> el :attrs :points)))]))

(defmethod element.hierarchy/edit ::element.hierarchy/polyshape
  [el [x y] handle]
  (let [index (js/parseInt (name handle))
        transform-point (fn [[px py]]
                          (list (utils.length/transform px + x)
                                (utils.length/transform py + y)))]
    (update-in el [:attrs :points] #(-> (utils.attribute/points->vec %)
                                        (update index transform-point)
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

(defn ->vertices
  [el]
  (-> el :attrs :points points->px))

(defmethod element.hierarchy/area ::element.hierarchy/polyshape
  [el]
  (let [vertices (->vertices el)
        count-v (count vertices)]
    (/ (reduce-kv (fn [area index point]
                    (let [point-b (if (= index (dec count-v))
                                    (first vertices)
                                    (nth vertices (inc index)))]
                      (+ area
                         (- (* (first point) (second point-b))
                            (* (first point-b) (second point))))))
                  0
                  vertices) 2)))

(defmethod element.hierarchy/centroid ::element.hierarchy/polyshape
  [el]
  (let [vertices (->vertices el)]
    (-> (reduce matrix/add [0 0] vertices)
        (matrix/div (count vertices)))))

(defmethod element.hierarchy/snapping-points ::element.hierarchy/polyshape
  [el]
  (->vertices el))
