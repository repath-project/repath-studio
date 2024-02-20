(ns renderer.tools.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   ["polylabel" :as polylabel]
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.attribute.utils :as attr.utils]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as h]
   [renderer.history.handlers :as history]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive ::tools/polyshape ::tools/shape)

(defmethod tools/activate ::tools/polyshape
  [db]
  (-> db
      (assoc :cursor "crosshair")
      (h/set-message
       [:div
        [:div "Click to add points."]
        [:div "Double click to finalize the shape."]])))

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

(defmethod tools/pointer-up ::tools/polyshape
  [{:keys [adjusted-pointer-pos] :as db}]
  (if (element.h/get-temp db)
    (add-point db adjusted-pointer-pos)
    (-> db
        (h/set-state :create)
        (create-polyline adjusted-pointer-pos))))

(defmethod tools/drag-end ::tools/polyshape
  [{:keys [adjusted-pointer-pos] :as db}]
  (if (element.h/get-temp db)
    (add-point db adjusted-pointer-pos)
    (-> db
        (h/set-state :create)
        (create-polyline adjusted-pointer-pos))))

(defmethod tools/pointer-move ::tools/polyshape
  [{:keys [active-document adjusted-pointer-pos] :as db}]
  (if-let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (let [point-vector (attr.utils/points->vec points)]
      (assoc-in db
                [:documents active-document :temp-element :attrs :points]
                (str/join " " (concat (apply concat (if (second point-vector)
                                                      (drop-last point-vector)
                                                      point-vector))
                                      adjusted-pointer-pos)))) db))

(defmethod tools/double-click ::tools/polyshape
  [{:keys [active-document] :as db}]
  (-> db
      (update-in [:documents active-document :temp-element :attrs :points]
                 #(str/join " " (apply concat (drop-last 2 (attr.utils/points->vec %)))))
      element.h/add
      (tools/set-tool :select)
      (h/set-state :default)
      (history/finalize "Create " (:tool db))))

(defmethod tools/translate ::tools/polyshape
  [el [x y]]
  (update-in el
             [:attrs :points]
             #(->> %
                   attr.utils/points->vec
                   (reduce (fn [points point]
                             (conj points
                                   (units/transform (first point) + x)
                                   (units/transform (second point) + y))) [])
                   (str/join " "))))

(defmethod tools/position ::tools/polyshape
  [el position]
  (let [center (bounds/center (tools/bounds el))
        [x y] (mat/sub position center)]
    (update-in el
               [:attrs :points]
               #(->> %
                     attr.utils/points->vec
                     (reduce (fn [points point]
                               (conj points
                                     (units/transform (first point) + x)
                                     (units/transform (second point) + y))) [])
                     (str/join " ")))))

(defmethod tools/scale ::tools/polyshape
  [el ratio pivot-point]
  (let [bounds-start (take 2 (tools/bounds el))
        pivot-point (mat/sub pivot-point (mat/mul pivot-point ratio))]
    (update-in el
               [:attrs :points]
               #(->> %
                     attr.utils/points->vec
                     (reduce (fn [points point]
                               (let [[point-x point-y] point
                                     rel-point (mat/sub (take 2 bounds-start) point)
                                     [x y] (mat/add pivot-point (mat/sub rel-point (mat/mul rel-point ratio)))]
                                 (conj points
                                       (units/transform point-x + x)
                                       (units/transform point-y + y)))) [])
                     (str/join " ")))))

(defmethod tools/render-edit ::tools/polyshape
  [{:keys [attrs key] :as el} zoom]
  (let [{:keys [points]} attrs
        handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        offset @(rf/subscribe [:element/el-offset el])]
    [:g {:key :edit-handlers}
     (map-indexed (fn [index [x y]]
                    (let [[x y] (mapv units/unit->px [x y])
                          [x y] (mat/add offset [x y])]
                      [overlay/square-handler {:key (str index)
                                               :x x
                                               :y y
                                               :size handler-size
                                               :stroke-width stroke-width
                                               :type :handler
                                               :tag :edit
                                               :element key}]))
                  (attr.utils/points->vec points))]))

(defmethod tools/edit ::tools/polyshape
  [el [x y] handler]
  (cond-> el
    (not (keyword? handler))
    (update-in
     [:attrs :points]
     #(str/join " "
                (-> (attr.utils/points->vec %1)
                    (update (int handler)
                            (fn [point]
                              (list
                               (units/transform (first point) + x)
                               (units/transform (second point) + y))))
                    flatten)))))

(defmethod tools/bounds ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr.utils/points->vec points)
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

(defmethod tools/area ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr.utils/points->px points)]
    (calc-polygon-area points-v)))

(defmethod tools/centroid ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr.utils/points->px points)]
    (mat/div (reduce mat/add [0 0] points-v)
             (count points-v))))

(defmethod tools/poi ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr.utils/points->px points)]
    (take 2 (polylabel (clj->js [points-v])))))
