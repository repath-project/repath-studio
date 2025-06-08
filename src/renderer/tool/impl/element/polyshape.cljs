(ns renderer.tool.impl.element.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.string :as string]
   [renderer.document.handlers :as document.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.i18n :refer [t]]))

(derive ::tool.hierarchy/polyshape ::tool.hierarchy/element)

(defn points-path
  [db]
  [:documents (:active-document db) :temp-element :attrs :points])

(defmethod tool.hierarchy/help [::tool.hierarchy/polyshape :idle]
  []
  [:<>
   [:div (t [::add-points "Click to add more points."])]
   [:div (t [::finalize-shape "Double click to finalize the shape."])]])

(defn create-polyline
  [db points]
  (tool.handlers/set-temp db {:type :element
                              :tag (:tool db)
                              :attrs {:points (string/join " " points)
                                      :stroke (document.handlers/attr db :stroke)
                                      :fill (document.handlers/attr db :fill)}}))

(defn add-point
  [db point]
  (update-in db (points-path db) #(str % " " (string/join " " point))))

(defn drop-last-point
  [db]
  (let [points (get-in db (points-path db))
        point-vector (utils.attribute/points->vec points)]
    (assoc-in db
              (points-path db)
              (->> point-vector drop-last flatten (string/join " ")))))

(defmethod tool.hierarchy/on-pointer-up ::tool.hierarchy/polyshape
  [db _e]
  (let [point (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))]
    (if (tool.handlers/temp db)
      (add-point db point)
      (-> (tool.handlers/set-state db :create)
          (create-polyline point)))))

(defmethod tool.hierarchy/on-drag-end ::tool.hierarchy/polyshape
  [db _e]
  (if (tool.handlers/temp db)
    (add-point db (:adjusted-pointer-pos db))
    (-> (tool.handlers/set-state db :create)
        (create-polyline (:adjusted-pointer-pos db)))))

(defmethod tool.hierarchy/on-pointer-move ::tool.hierarchy/polyshape
  [db _e]
  (let [point (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))]
    (if-let [points (get-in db (points-path db))]
      (let [point-vector (utils.attribute/points->vec points)]
        (assoc-in db
                  (points-path db)
                  (string/join " " (concat (apply concat (if (second point-vector)
                                                           (drop-last point-vector)
                                                           point-vector))
                                           point)))) db)))

(defmethod tool.hierarchy/on-double-click ::tool.hierarchy/polyshape
  [db _e]
  (-> (drop-last-point db)
      (tool.handlers/create-temp-element)
      (tool.handlers/activate :transform)
      (history.handlers/finalize (str "Create " (name (:tool db))))))
