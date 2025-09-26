(ns renderer.tool.impl.element.poly
  "This serves as an abstraction for polygons and polylines that have similar
   attributes and hehavior"
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.attribute :as utils.attribute]
   [renderer.utils.i18n :refer [t]]))

(derive ::tool.hierarchy/poly ::tool.hierarchy/element)

(defmethod tool.hierarchy/help [::tool.hierarchy/poly :idle]
  []
  [:<>
   [:div (t [::add-points "Click to add more points."])]
   [:div (t [::finalize-shape "Double click to finalize the shape."])]])

(defn create-polyline
  [db initial-point]
  (let [stroke (document.handlers/attr db :stroke)
        fill (document.handlers/attr db :fill)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag (:tool db)
                               :attrs {:points (string/join " " initial-point)
                                       :stroke stroke
                                       :fill fill}}))))

(defn creating-el
  [db]
  (-> db element.handlers/selected first))

(defn add-point
  [db point]
  (let [id (:id (creating-el db))]
    (if (= (:state db) :create)
      (element.handlers/update-attr db id
                                    :points
                                    str " " (string/join " " point))
      (create-polyline db point))))

(defn drop-last-point
  [db]
  (let [id (:id (creating-el db))]
    (element.handlers/update-attr db id
                                  :points
                                  #(->> (utils.attribute/points->vec %)
                                        (drop-last)
                                        (flatten)
                                        (string/join " ")))))

(defn adjusted-point
  [db point]
  (let [parent-id (:parent (creating-el db))
        parent-el (element.handlers/entity db parent-id)
        [min-x min-y] (element.hierarchy/bbox parent-el)]
    (matrix/sub point [min-x min-y])))

(defmethod tool.hierarchy/on-pointer-up ::tool.hierarchy/poly
  [db _e]
  (let [point (or (:point (:nearest-neighbor db))
                  (:adjusted-pointer-pos db))
        point (cond->> point
                (= (:state db) :create)
                (adjusted-point db))]
    (add-point db point)))

(defmethod tool.hierarchy/on-drag-end ::tool.hierarchy/poly
  [db e]

  (tool.hierarchy/on-pointer-up db e))

(defmethod tool.hierarchy/on-pointer-move ::tool.hierarchy/poly
  [db _e]
  (let [point (or (:point (:nearest-neighbor db))
                  (:adjusted-pointer-pos db))
        point (adjusted-point db point)
        id (:id (first (element.handlers/selected db)))]
    (cond-> db
      (= (:state db) :create)
      (element.handlers/update-attr
       id :points
       #(let [point-vector (utils.attribute/points->vec %)
              point-vector (cond-> point-vector
                             (second point-vector)
                             (drop-last))]
          (->> (concat point-vector point)
               (flatten)
               (string/join " ")))))))

(defmethod tool.hierarchy/on-double-click ::tool.hierarchy/poly
  [db _e]
  (-> db
      (drop-last-point)
      (history.handlers/finalize [::create-tool "Create %1"] [(name (:tool db))])
      (tool.handlers/activate :transform)))
