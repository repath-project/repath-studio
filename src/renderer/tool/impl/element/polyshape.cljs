(ns renderer.tool.impl.element.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.string :as str]
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.attribute :as attr]))

(derive ::hierarchy/polyshape ::hierarchy/element)

(defn points-path
  [db]
  [:documents (:active-document db) :temp-element :attrs :points])

(defmethod hierarchy/help [::hierarchy/polyshape :idle]
  []
  "Click to add more points. Double click to finalize the shape.")

(defn create-polyline
  [db points]
  (h/set-temp db {:type :element
                  :tag (:tool db)
                  :attrs {:points (str/join " " points)
                          :stroke (document.h/attr db :stroke)
                          :fill (document.h/attr db :fill)}}))

(defn add-point
  [db point]
  (update-in db (points-path db) #(str % " " (str/join " " point))))

(defn drop-last-point
  [db]
  (let [points (get-in db (points-path db))
        point-vector (attr/points->vec points)]
    (assoc-in db
              (points-path db)
              (->> point-vector drop-last flatten (str/join " ")))))

(defmethod hierarchy/pointer-up ::hierarchy/polyshape
  [db]
  (let [point (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))]
    (if (h/temp db)
      (add-point db point)
      (-> (h/set-state db :create)
          (create-polyline point)))))

(defmethod hierarchy/drag-end ::hierarchy/polyshape
  [db]
  (if (h/temp db)
    (add-point db (:adjusted-pointer-pos db))
    (-> (h/set-state db :create)
        (create-polyline (:adjusted-pointer-pos db)))))

(defmethod hierarchy/pointer-move ::hierarchy/polyshape
  [db]
  (let [point (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))]
    (if-let [points (get-in db (points-path db))]
      (let [point-vector (attr/points->vec points)]
        (assoc-in db
                  (points-path db)
                  (str/join " " (concat (apply concat (if (second point-vector)
                                                        (drop-last point-vector)
                                                        point-vector))
                                        point)))) db)))

(defmethod hierarchy/double-click ::hierarchy/polyshape
  [db _e]
  (-> (drop-last-point db)
      (h/create-temp-element)
      (h/activate :transform)
      (history.h/finalize (str "Create " (name (:tool db))))))
