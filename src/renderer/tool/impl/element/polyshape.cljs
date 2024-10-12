(ns renderer.tool.impl.element.polyshape
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.attribute :as utils.attr]))

(derive ::tool.hierarchy/polyshape ::tool.hierarchy/element)

(defn points-path
  [db]
  [:documents (:active-document db) :temp-element :attrs :points])

(defmethod tool.hierarchy/help [::tool.hierarchy/polyshape :default]
  []
  "Click to add more points. Double click to finalize the shape.")

(defmethod tool.hierarchy/activate ::tool.hierarchy/polyshape
  [db]
  (app.h/set-cursor db "crosshair"))

(defn create-polyline
  [db points]
  (let [{:keys [fill stroke]} (get-in db [:documents (:active-document db)])]
    (element.h/assoc-temp db {:type :element
                              :tag (:tool db)
                              :attrs {:points (str/join " " points)
                                      :stroke stroke
                                      :fill fill}})))

(defn add-point
  [db point]
  (update-in db (points-path db) #(str % " " (str/join " " point))))

(defmethod tool.hierarchy/pointer-up ::tool.hierarchy/polyshape
  [db]
  (if (element.h/get-temp db)
    (add-point db (:adjusted-pointer-pos db))
    (-> db
        (app.h/set-state :create)
        (create-polyline (:adjusted-pointer-pos db)))))

(defmethod tool.hierarchy/drag-end ::tool.hierarchy/polyshape
  [db]
  (if (element.h/get-temp db)
    (add-point db (:adjusted-pointer-pos db))
    (-> db
        (app.h/set-state :create)
        (create-polyline (:adjusted-pointer-pos db)))))

(defmethod tool.hierarchy/pointer-move ::tool.hierarchy/polyshape
  [db]
  (if-let [points (get-in db (points-path db))]
    (let [point-vector (utils.attr/points->vec points)]
      (assoc-in db
                (points-path db)
                (str/join " " (concat (apply concat (if (second point-vector)
                                                      (drop-last point-vector)
                                                      point-vector))
                                      (:adjusted-pointer-pos db))))) db))

(defmethod tool.hierarchy/double-click ::tool.hierarchy/polyshape
  [db _e]
  (-> db
      (update-in (points-path db) #(->> (utils.attr/points->vec %)
                                        (drop-last)
                                        (apply concat)
                                        (str/join " ")))
      (element.h/add)
      (app.h/set-tool :select)
      (app.h/set-state :default)
      (app.h/explain "Create " (name (:tool db)))))
