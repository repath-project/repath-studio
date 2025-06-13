(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as matrix]
   [kdtree :as kdtree]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.ruler.handlers :as ruler.handlers]
   [renderer.snap.db :refer [SnapOption NearestNeighbor]]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.math :refer [Vec2]]))

(m/=> toggle-option [:-> App SnapOption App])
(defn toggle-option
  [db option]
  (if (contains? (-> db :snap :options) option)
    (update-in db [:snap :options] disj option)
    (update-in db [:snap :options] conj option)))

(m/=> nearest-neighbors [:-> App [:sequential NearestNeighbor]])
(defn nearest-neighbors
  [db]
  (->> (tool.hierarchy/snapping-points db)
       (keep #(when-let [nneighbor (kdtree/nearest-neighbor (:viewbox-kdtree db) %)]
                (assoc nneighbor :base-point %)))))

(m/=> update-nearest-neighbors [:-> App App])
(defn update-nearest-neighbors
  [db]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        threshold (-> db :snap :threshold)
        threshold (Math/pow (/ threshold zoom) 2)
        nneighbors (nearest-neighbors db)
        nneighbors (filter #(< (:dist-squared %) threshold) nneighbors)]
    (assoc db
           :nearest-neighbors nneighbors
           :nearest-neighbor (apply min-key :dist-squared nneighbors))))

(m/=> update-viewport-tree [:-> App App])
(defn update-viewport-tree
  [db]
  (let [[x y width height] (frame.handlers/viewbox db)
        boundaries [[x (+ x width)] [y (+ y height)]]]
    (assoc db :viewbox-kdtree (-> (:kdtree db)
                                  (kdtree/interval-search boundaries)
                                  (kdtree/build-tree)))))

(m/=> rebuild-tree [:-> App App])
(defn rebuild-tree
  [db]
  (if (-> db :snap :active)
    (let [elements (tool.hierarchy/snapping-elements db)
          points (element.handlers/snapping-points db elements)
          points (cond-> points
                   (contains? (-> db :snap :options) :grid)
                   (into (ruler.handlers/steps-intersections db)))]
      (-> (assoc db :kdtree (kdtree/build-tree points))
          (update-viewport-tree)))
    (dissoc db :kdtree :viewbox-kdtree)))

(m/=> update-tree [:-> App ifn? [:vector Vec2] App])
(defn update-tree
  [db f points]
  (if (:kdtree db)
    (if (empty? points)
      db
      (-> (reduce #(update %1 :kdtree f %2) db points)
          (update-viewport-tree)))
    (rebuild-tree db)))

(m/=> insert-to-tree [:-> App [:maybe [:set uuid?]] App])
(defn insert-to-tree
  [db element-ids]
  (let [elements (vals (element.handlers/entities db element-ids))
        points (element.handlers/snapping-points db elements)]
    (update-tree db kdtree/insert points)))

(m/=> delete-from-tree [:-> App [:maybe [:set uuid?]] App])
(defn delete-from-tree
  [db element-ids]
  (let [elements (vals (element.handlers/entities db element-ids))
        points (element.handlers/snapping-points db elements)]
    (update-tree db kdtree/delete points)))

(m/=> nearest-delta [:-> App Vec2])
(defn nearest-delta
  [db]
  (if (:nearest-neighbor db)
    (let [{:keys [point base-point]} (:nearest-neighbor db)]
      (matrix/sub point base-point))
    [0 0]))

(m/=> snap-with [:-> App ifn? [:* any?] App])
(defn snap-with
  [db f & more]
  (if (-> db :snap :active)
    (let [db (update-nearest-neighbors db)]
      (if (:nearest-neighbor db)
        (apply f db (nearest-delta db) more)
        db))
    db))
