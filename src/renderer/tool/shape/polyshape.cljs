(ns renderer.tool.shape.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.attribute :as utils.attr]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive ::tool.hierarchy/polyshape ::tool.hierarchy/shape)

(defmethod tool.hierarchy/help [::tool.hierarchy/polyshape :default]
  []
  "Click to add more points. Double click to finalize the shape.")

(defmethod tool.hierarchy/activate ::tool.hierarchy/polyshape
  [db]
  (assoc db :cursor "crosshair"))

(defn create-polyline
  [{:keys [active-document tool] :as db} points]
  (let [{:keys [fill stroke]} (get-in db [:documents active-document])]
    (element.h/assoc-temp db {:type :element
                              :tag tool
                              :attrs {:points (str/join " " points)
                                      :stroke stroke
                                      :fill fill}})))

(defn add-point
  [{:keys [active-document] :as db} point]
  (update-in db
             [:documents active-document :temp-element :attrs :points]
             #(str % " " (str/join " " point))))

(defmethod tool.hierarchy/pointer-up ::tool.hierarchy/polyshape
  [{:keys [adjusted-pointer-pos] :as db}]
  (if (element.h/get-temp db)
    (add-point db adjusted-pointer-pos)
    (-> db
        (app.h/set-state :create)
        (create-polyline adjusted-pointer-pos))))

(defmethod tool.hierarchy/drag-end ::tool.hierarchy/polyshape
  [{:keys [adjusted-pointer-pos] :as db}]
  (if (element.h/get-temp db)
    (add-point db adjusted-pointer-pos)
    (-> db
        (app.h/set-state :create)
        (create-polyline adjusted-pointer-pos))))

(defmethod tool.hierarchy/pointer-move ::tool.hierarchy/polyshape
  [{:keys [active-document adjusted-pointer-pos] :as db}]
  (if-let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (let [point-vector (utils.attr/points->vec points)]
      (assoc-in db
                [:documents active-document :temp-element :attrs :points]
                (str/join " " (concat (apply concat (if (second point-vector)
                                                      (drop-last point-vector)
                                                      point-vector))
                                      adjusted-pointer-pos)))) db))

(defmethod tool.hierarchy/double-click ::tool.hierarchy/polyshape
  [{:keys [active-document] :as db} _e]
  (-> db
      (update-in [:documents active-document :temp-element :attrs :points]
                 #(->> %
                       (utils.attr/points->vec)
                       (drop-last)
                       (apply concat)
                       (str/join " ")))
      (element.h/add)
      (app.h/set-tool :select)
      (app.h/set-state :default)
      (app.h/explain "Create " (name (:tool db)))))

(defn translate
  [[offset-x offset-y] points [point-x point-y]]
  (conj points
        (when point-x (units/transform point-x + offset-x))
        (when point-y (units/transform point-y + offset-y))))

(defmethod tool.hierarchy/translate ::tool.hierarchy/polyshape
  [el offset]
  (update-in el
             [:attrs :points]
             #(->> (utils.attr/str->seq %)
                   (transduce (partition-all 2) (partial translate offset) [])
                   (str/join " "))))

(defmethod tool.hierarchy/scale ::tool.hierarchy/polyshape
  [el ratio pivot-point]
  (let [bounds-start (take 2 (tool.hierarchy/bounds el))
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

(defmethod tool.hierarchy/render-edit ::tool.hierarchy/polyshape
  [{:keys [attrs id] :as el} zoom]
  (let [{:keys [points]} attrs
        handle-size (/ 8 zoom)
        stroke-width (/ 1 zoom)
        offset (element/offset el)]
    [:g
     (map-indexed (fn [index [x y]]
                    (let [[x y] (mapv units/unit->px [x y])
                          [x y] (mat/add offset [x y])]
                      ^{:key index}
                      [overlay/square-handle {:id (keyword (str index))
                                              :x x
                                              :y y
                                              :size handle-size
                                              :stroke-width stroke-width
                                              :type :handle
                                              :cursor "move"
                                              :tag :edit
                                              :element id}]))
                  (utils.attr/points->vec points))]))

(defmethod tool.hierarchy/edit ::tool.hierarchy/polyshape
  [el [x y] handle]
  (update-in
   el
   [:attrs :points]
   #(-> (utils.attr/points->vec %1)
        (update (js/parseInt (name handle))
                (fn [point]
                  (list
                   (units/transform (first point) + x)
                   (units/transform (second point) + y))))
        (flatten)
        (->> (str/join " ")))))

(defmethod tool.hierarchy/bounds ::tool.hierarchy/polyshape
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

(defmethod tool.hierarchy/area ::tool.hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->px points)]
    (calc-polygon-area points-v)))

(defmethod tool.hierarchy/centroid ::tool.hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (let [points-v (utils.attr/points->px points)]
    (mat/div (reduce mat/add [0 0] points-v)
             (count points-v))))

(defmethod tool.hierarchy/snapping-points ::tool.hierarchy/polyshape
  [{{:keys [points]} :attrs}]
  (utils.attr/points->px points))
