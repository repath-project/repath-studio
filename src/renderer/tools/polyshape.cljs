(ns renderer.tools.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [renderer.elements.handlers :as elements]
   [renderer.overlay :as overlay]
   [renderer.tools.base :as tools]
   [renderer.utils.units :as units]
   [renderer.history.handlers :as history]
   [renderer.handlers :as handlers]
   [re-frame.core :as rf]
   [clojure.string :as str]
   [clojure.core.matrix :as matrix]
   [renderer.attribute.utils :as attr-utils]
   ["polylabel" :as polylabel]))

(derive ::tools/polyshape ::tools/shape)

(defmethod tools/activate ::tools/polyshape
  [db]
  (-> db
      (handlers/set-message
       [:div
        [:div "Click to add points."]
        [:div "Double click to finalize the shape."]])))

(defn create-polyline
  [{:keys [active-document tool] :as db} points]
  (let [{:keys [fill stroke]} (get-in db [:documents active-document])]
    (elements/set-temp db {:type :element
                           :tag tool
                           :attrs {:points (str/join " " points)
                                   :stroke stroke
                                   :fill fill}})))

(defn add-point
  [{:keys [active-document] :as db} point]
  (update-in db
             [:documents active-document :temp-element :attrs :points]
             #(str % " " (str/join " " point))))

(defmethod tools/mouse-up ::tools/polyshape
  [{:keys [adjusted-mouse-pos] :as db}]
  (if (elements/get-temp db)
    (add-point db adjusted-mouse-pos)
    (-> db
        (handlers/set-state :create)
        (create-polyline adjusted-mouse-pos))))

(defmethod tools/drag-end ::tools/polyshape
  [{:keys [adjusted-mouse-pos] :as db}]
  (if (elements/get-temp db)
    (add-point db adjusted-mouse-pos)
    (-> db
        (handlers/set-state :create)
        (create-polyline adjusted-mouse-pos))))

(defmethod tools/mouse-move ::tools/polyshape
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (if-let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (let [point-vector (attr-utils/points-to-vec points)]
      (assoc-in db
                [:documents active-document :temp-element :attrs :points]
                (str/join " " (concat (apply concat (if (second point-vector)
                                                      (drop-last point-vector)
                                                      point-vector))
                                      adjusted-mouse-pos)))) db))

(defmethod tools/double-click ::tools/polyshape
  [{:keys [active-document] :as db}]
  (-> db
      (update-in [:documents active-document :temp-element :attrs :points]
                 #(str/join " " (apply concat (drop-last 2 (attr-utils/points-to-vec %)))))
      (elements/create-from-temp)
      (history/finalize (str "Create " (:tool db)))))

(defmethod tools/translate ::tools/polyshape
  [element [x y]]
  (update-in element
             [:attrs :points]
             #(->> %1
                   (attr-utils/points-to-vec)
                   (reduce (fn [points point]
                             (conj points
                                   (units/transform + x (first point))
                                   (units/transform + y (second point)))) [])
                   (str/join " "))))


(defmethod tools/render-edit ::tools/polyshape
  [{:keys [attrs key]} zoom]
  (let [{:keys [points]} attrs
        handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        active-page @(rf/subscribe [:elements/active-page])
        page-pos (mapv units/unit->px
                       [(-> active-page :attrs :x) (-> active-page :attrs :y)])]
    [:g {:key :edit-handlers}
     (map-indexed (fn [index [x y]]
                    (let [[x y] (mapv units/unit->px [x y])
                          [x y] (matrix/add page-pos [x y])]
                      [overlay/square-handler {:key (str index)
                                               :x x
                                               :y y
                                               :size handler-size
                                               :stroke-width stroke-width
                                               :type :handler
                                               :tag :edit
                                               :element key}]))
                  (attr-utils/points-to-vec points))]))

(defmethod tools/edit ::tools/polyshape
  [element [x y] handler]
  (if (not (keyword? handler))
    (update-in element
               [:attrs :points]
               #(str/join " "
                          (-> (attr-utils/points-to-vec %1)
                              (update (int handler)
                                      (fn [point]
                                        (list
                                         (units/transform + x (first point))
                                         (units/transform + y (second point)))))
                              (flatten))))
    element))

(defmethod tools/bounds ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr-utils/points-to-vec points)
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
  (let [points-v (attr-utils/points-to-vec points)
        points-v (mapv (fn [point] (mapv units/unit->px point)) points-v)]
    (calc-polygon-area points-v)))

(defmethod tools/centroid ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr-utils/points-to-vec points)
        points-v (mapv (fn [point] (mapv units/unit->px point)) points-v)]
    (-> (reduce matrix/add [0 0] points-v)
        (matrix/div (count points-v)))))

(defmethod tools/poi ::tools/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (attr-utils/points-to-vec points)
        points-v (mapv (fn [point] (mapv units/unit->px point)) points-v)]
    (take 2 (polylabel (clj->js [points-v])))))




