(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree :as kdtree]
   [malli.core :as m]
   [renderer.app.db :refer [App]]
   [renderer.frame.db :refer [Viewbox]]
   [renderer.frame.handlers :as frame.h]
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

(m/=> in-viewport-tree [:-> any? Viewbox any?])
(defn in-viewport-tree
  [tree [x y width height]]
  (->> [[x (+ x width)] [y (+ y height)]]
       (kdtree/interval-search tree)
       (kdtree/build-tree)))

(m/=> in-viewport-tree [:-> [:vector Vec2D] Viewbox any?])
(defn create-tree
  [points [x y width height]]
  (->  (kdtree/build-tree points)
       (kdtree/interval-search [[x (+ x width)] [y (+ y height)]])
       (kdtree/build-tree)))

(m/=> nearest-neighbors [:-> App [:sequential NearestNeighbor]])
(defn nearest-neighbors
  [db]
  (map #(when-let [nneighbor (kdtree/nearest-neighbor (:kd-tree db) %)]
          (assoc nneighbor :base-point %))
       (tool.hierarchy/snapping-bases db)))

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

(m/=> update-tree [:-> App App])
(defn update-tree
  [db]
  (if (-> db :snap :active)
    (let [zoom (get-in db [:documents (:active-document db) :zoom])
          pan (get-in db [:documents (:active-document db) :pan])
          viewbox (frame.h/viewbox zoom pan (:dom-rect db))
          points (tool.hierarchy/snapping-points db)]
      (assoc db
             :snapping-points points
             :kd-tree (create-tree points viewbox)))
    (dissoc db :kd-tree)))

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
