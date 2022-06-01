(ns repath.studio.tools.polygon
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [repath.studio.attrs.base :as attrs]
            [repath.studio.history.handlers :as history]
            [repath.studio.handlers :as handlers]
            [clojure.string :as str]))

(derive :polygon ::tools/shape)

(defmethod tools/properties :polygon [] {:icon "polygon"
                                         :description "The <polyline> SVG element is an SVG basic shape that creates straight lines connecting several points."
                                         :attrs [:stroke-width
                                                 :fill
                                                 :stroke
                                                 :stroke-linejoin
                                                 :opacity]})

(defn create-polyline
  [{:keys [active-document] :as db} points]
  (let [{:keys [fill]} (get-in db [:documents active-document])]
    (elements/set-temp db {:type :element :tag :polygon :attrs {:points (str/join " " points)
                                                                :stroke (tools/rgba (get-in db [:documents active-document :stroke]))
                                                                :fill   (tools/rgba fill)}})))

(defn add-point
  [{:keys [active-document] :as db} point]
  (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " point))))

(defmethod tools/mouse-up :polygon
  [{:keys [adjusted-mouse-pos] :as db}]
  (if (elements/get-temp db)
    (add-point db adjusted-mouse-pos)
    (-> db
        (handlers/set-state :create)
        (create-polyline adjusted-mouse-pos))))

(defmethod tools/drag-start :polygon
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (if (elements/get-temp db)
    (add-point db (concat adjusted-mouse-offset adjusted-mouse-pos))
    (create-polyline db (concat adjusted-mouse-offset adjusted-mouse-pos))))

(defmethod tools/drag-end :polygon
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (if (elements/get-temp db)
    (add-point db adjusted-mouse-pos)
    (create-polyline db (concat adjusted-mouse-offset adjusted-mouse-pos))))

(defmethod tools/drag :polygon
  [db]
  (tools/mouse-move db))

(defmethod tools/mouse-move :polygon
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (if-let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (let [point-vector (attrs/points-to-vec points)]
      (assoc-in db [:documents active-document :temp-element :attrs :points] (str/join " " (concat (apply concat (if (second point-vector) (drop-last point-vector) point-vector)) adjusted-mouse-pos)))) db))

(defmethod tools/double-click :polygon
  [{:keys [active-document] :as db}]
  (-> db
      (update-in [:documents active-document :temp-element :attrs :points] #(str/join " " (apply concat (drop-last 2 (attrs/points-to-vec %)))))
      (elements/create-from-temp)
      (history/finalize (str "Create polyline"))))

(defmethod tools/translate :polygon
  [element [x y]] (update-in element [:attrs :points] #(->> %
                                                            (attrs/points-to-vec)
                                                            (reduce (fn [points point] (concat points [(units/transform + x (first point)) (units/transform + y (second point))])) [])
                                                            (concat)
                                                            (str/join " "))))

(defmethod tools/render-edit :polygon
  [{{:keys [points]} :attrs}]
  [:g {:key :edit-handlers}
   (map element-views/square-handler (map-indexed (fn [index [x y]] {:x x
                                                                     :y y
                                                                     :key (keyword index)
                                                                     :type :handler
                                                                     :tag :edit}) (attrs/points-to-vec points)))])

(defmethod tools/bounds :polygon
  [{{:keys [points]} :attrs}]
  (let [points-v (attrs/points-to-vec points)
        x1 (apply min (map #(units/unit->px (first %)) points-v))
        y1 (apply min (map #(units/unit->px (second %)) points-v))
        x2 (apply max (map #(units/unit->px (first %)) points-v))
        y2 (apply max (map #(units/unit->px (second %)) points-v))]
    [x1 y1 x2 y2]))

