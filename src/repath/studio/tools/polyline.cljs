(ns repath.studio.tools.polyline
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [repath.studio.attrs.base :as attrs]
            [repath.studio.history.handlers :as history]
            [repath.studio.handlers :as handlers]
            [clojure.string :as str]))

(derive :polyline ::tools/shape)

(defmethod tools/properties :polyline [] {:icon "polyline"
                                          :description "The <polyline> SVG element is an SVG basic shape that creates straight lines connecting several points."
                                          :attrs [:stroke-width
                                                  :fill
                                                  :stroke
                                                  :stroke-linejoin
                                                  :opacity]})

(defn create-polyline
  [{:keys [active-document] :as db} points]
  (elements/set-temp db {:type :polyline :attrs {:points (str/join " " points)
                                                 :stroke (tools/rgba (get-in db [:documents active-document :stroke]))
                                                 :fill "transparent"}}))

(defn add-point
  [{:keys [active-document] :as db} point]
  (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " point))))

(defmethod tools/mouse-up :polyline
  [{:keys [adjusted-mouse-offset] :as db}]
  (if (elements/get-temp db)
    (add-point db adjusted-mouse-offset)
    (-> db
        (handlers/set-state :create)
        (create-polyline adjusted-mouse-offset))))

(defmethod tools/drag-start :polyline
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (if (elements/get-temp db)
    #_(add-point db (concat adjusted-mouse-offset adjusted-mouse-pos))
    db
    (create-polyline db (concat adjusted-mouse-offset adjusted-mouse-pos))))

(defmethod tools/drag-end :polyline
  [{:keys [adjusted-mouse-pos adjusted-mouse-offset] :as db}]
  (if (elements/get-temp db)
    (add-point db adjusted-mouse-pos)
    (create-polyline db (concat adjusted-mouse-offset adjusted-mouse-pos))))

(defmethod tools/drag :polyline
  [db]
  (tools/mouse-move db))

(defmethod tools/mouse-move :polyline
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (if-let [points (get-in db [:documents active-document :temp-element :attrs :points])]
    (let [point-vector (attrs/points-to-vec points)]
      (assoc-in db [:documents active-document :temp-element :attrs :points] (str/join " " (concat (apply concat (if (second point-vector) (drop-last point-vector) point-vector)) adjusted-mouse-pos)))) db))

(defmethod tools/double-click :polyline
  [{:keys [active-document] :as db}]
  (-> db
      (update-in [:documents active-document :temp-element :attrs :points] #(str/join " " (apply concat (drop-last 2 (attrs/points-to-vec %)))))
      (elements/create-from-temp)
      (history/finalize (str "Create polyline"))))

(defmethod tools/translate :polyline
  [element [x y]] (update-in element [:attrs :points] #(->> %
                                                            (attrs/points-to-vec)
                                                            (mapv (fn [point] [(units/transform + x (first point)) (units/transform + y (second point))]))
                                                            (flatten)
                                                            (str/join " "))))

(defmethod tools/render-edit :polyline
  [{{:keys [points]} :attrs}]
  [:g {:key :edit-handlers}
   (map element-views/square-handler (map-indexed (fn [index [x y]] {:x x
                                                                     :y y
                                                                     :key (keyword index)
                                                                     :type :edit-handler}) (attrs/points-to-vec points)))])

(defmethod tools/bounds :polyline
  [{{:keys [points]} :attrs}]
  (let [points-v (attrs/points-to-vec points)
        x1 (apply min (map #(units/unit->px (first %)) points-v))
        y1 (apply min (map #(units/unit->px (second %)) points-v))
        x2 (apply max (map #(units/unit->px (first %)) points-v))
        y2 (apply max (map #(units/unit->px (second %)) points-v))]
    [x1 y1 x2 y2]))

(defmethod tools/path :line
  [{{:keys [points]} :attrs}]
  (reduce #(str %1 "M " (str/join "," %2)) "" (attrs/points-to-vec points)))
