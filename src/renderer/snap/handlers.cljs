(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree :as kdtree]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.element.handlers :as element.h]
   [renderer.frame.db :refer [Viewbox]]
   [renderer.frame.handlers :as frame.h]
   [renderer.ruler.handlers :as ruler.h]
   [renderer.snap.db :refer [SnapOption NearestNeighbor]]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.math :refer [Vec2D]]))

(m/=> toggle-option [:-> App SnapOption App])
(defn toggle-option
  [db option]
  (if (contains? (-> db :snap :options) option)
    (update-in db [:snap :options] disj option)
    (update-in db [:snap :options] conj option)))

(m/=> viewport-tree [:-> any? Viewbox any?])
(defn viewport-tree
  [tree [x y width height]]
  (->> [[x (+ x width)] [y (+ y height)]]
       (kdtree/interval-search tree)
       (kdtree/build-tree)))

(m/=> nearest-neighbors [:-> App [:sequential NearestNeighbor]])
(defn nearest-neighbors
  [db]
  (map #(when-let [nneighbor (kdtree/nearest-neighbor (:viewbox-kdtree db) %)]
          (assoc nneighbor :base-point %))
       (tool.hierarchy/snapping-points db)))

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

(m/=> update-viewbox-tree [:-> App App])
(defn update-viewbox-tree
  [db]
  (assoc db :viewbox-kdtree (viewport-tree (:kdtree db) (frame.h/viewbox db))))

(m/=> update-tree [:-> App App])
(defn update-tree
  [db]
  (if (-> db :snap :active)
    (let [points (element.h/snapping-points db (tool.hierarchy/snapping-elements db))
          points (cond-> points
                   (contains? (-> db :snap :options) :grid)
                   (into (ruler.h/steps-intersections db)))]
      (-> (assoc db
                 :snapping-points points
                 :kdtree (kdtree/build-tree points))
          (update-viewbox-tree)))
    (dissoc db :kdtree :viewbox-kdtree :snapping-points)))

(m/=> nearest-delta [:-> App Vec2D])
(defn nearest-delta
  [db]
  (if (:nearest-neighbor db)
    (let [{:keys [point base-point]} (:nearest-neighbor db)]
      (mat/sub point base-point))
    [0 0]))

(defn snap-with
  [db f & more]
  (let [db (update-nearest-neighbors db)]
    (if (:nearest-neighbor db)
      (apply f db (nearest-delta db) more)
      db)))
