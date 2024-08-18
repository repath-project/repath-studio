(ns renderer.tool.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.attribute :as utils.attr]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive ::tool/polyshape ::tool/shape)

(defmethod tool/activate ::tool/polyshape
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (h/set-message "Click to add points. Double click to finalize the shape.")))

(defn create-polyline
  [{:keys [active-document tool] :as db} points]
  (let [{:keys [fill stroke]} (get-in db [:documents active-document])]
    (element.h/set-temp db {:type :element
                            :tag tool
                            :attrs {:points (str/join " " points)
                                    :stroke stroke
                                    :fill fill}})))

(defn add-point
  [{:keys [active-document] :as db} point]
  (update-in db
             [:documents active-document :temp-element :attrs :points]
             #(str % " " (str/join " " point))))

(defmethod tool/pointer-up ::tool/polyshape
  [{:keys [adjusted-pointer-pos] :as db}]
  (if (element.h/get-temp db)
    (add-point db adjusted-pointer-pos)
    (-> db
        (h/set-state :create)
        (create-polyline adjusted-pointer-pos))))

(defmethod tool/drag-end ::tool/polyshape
  [{:keys [adjusted-pointer-pos] :as db}]
  (if (element.h/get-temp db)
    (add-point db adjusted-pointer-pos)
    (-> db
        (h/set-state :create)
        (create-polyline adjusted-pointer-pos))))

(defmethod tool/pointer-move ::tool/polyshape
  [{:keys [active-document adjusted-pointer-pos] :as db}]
  (if-let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (let [point-vector (utils.attr/points->vec points)]
      (assoc-in db
                [:documents active-document :temp-element :attrs :points]
                (str/join " " (concat (apply concat (if (second point-vector)
                                                      (drop-last point-vector)
                                                      point-vector))
                                      adjusted-pointer-pos)))) db))

(defmethod tool/double-click ::tool/polyshape
  [{:keys [active-document] :as db}]
  (-> db
      (update-in [:documents active-document :temp-element :attrs :points]
                 #(str/join " " (apply concat (drop-last 2 (utils.attr/points->vec %)))))
      element.h/add
      (h/set-tool :select)
      (h/set-state :default)
      (history/finalize "Create " (name (:tool db)))))

(defmethod tool/translate ::tool/polyshape
  [el [x y]]
  (update-in el
             [:attrs :points]
             #(->> %
                   utils.attr/points->vec
                   (reduce (fn [points point]
                             (conj points
                                   (units/transform (first point) + x)
                                   (units/transform (second point) + y))) [])
                   (str/join " "))))

(defmethod tool/scale ::tool/polyshape
  [el ratio pivot-point]
  (let [bounds-start (take 2 (tool/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> %
                     utils.attr/points->vec
                     (reduce (fn [points point]
                               (let [[point-x point-y] point
                                     rel-point (mat/sub (take 2 bounds-start) point)
                                     [x y] (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                                 (conj points
                                       (units/transform point-x + x)
                                       (units/transform point-y + y)))) [])
                     (str/join " ")))))

(defmethod tool/render-edit ::tool/polyshape
  [{:keys [attrs key] :as el} zoom]
  (let [{:keys [points]} attrs
        handle-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        offset (element/offset el)]
    [:g {:key ::edit-handles}
     (map-indexed (fn [index [x y]]
                    (let [[x y] (mapv units/unit->px [x y])
                          [x y] (mat/add offset [x y])]
                      [overlay/square-handle {:key (str index)
                                              :x x
                                              :y y
                                              :size handle-size
                                              :stroke-width stroke-width
                                              :type :handle
                                              :tag :edit
                                              :element key}]))
                  (utils.attr/points->vec points))]))

(defmethod tool/edit ::tool/polyshape
  [el [x y] handle]
  (cond-> el
    (not (keyword? handle))
    (update-in
     [:attrs :points]
     #(str/join " "
                (-> (utils.attr/points->vec %1)
                    (update (int handle)
                            (fn [point]
                              (list
                               (units/transform (first point) + x)
                               (units/transform (second point) + y))))
                    flatten)))))

(defmethod tool/bounds ::tool/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->vec points)
        x1 (apply min (map #(units/unit->px (first %)) points-v))
        y1 (apply min (map #(units/unit->px (second %)) points-v))
        x2 (apply max (map #(units/unit->px (first %)) points-v))
        y2 (apply max (map #(units/unit->px (second %)) points-v))]
    [x1 y1 x2 y2]))

(defn calc-polygon-area [vertices]
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

(defmethod tool/area ::tool/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->px points)]
    (calc-polygon-area points-v)))

(defmethod tool/centroid ::tool/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->px points)]
    (mat/div (reduce mat/add [0 0] points-v)
             (count points-v))))

(defmethod tool/snapping-points ::tool/polyshape
  [{{:keys [points]} :attrs}]
  (utils.attr/points->px points))
