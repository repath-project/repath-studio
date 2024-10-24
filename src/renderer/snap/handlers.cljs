(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree :as kdtree]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
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

(m/=> find-nearest-neighbors [:-> App [:sequential NearestNeighbor]])
(defn find-nearest-neighbors
  [db]
  (let [tree @(rf/subscribe [::snap.s/in-viewport-tree])] ; FIXME: Subscription in event.
    (map #(let [nearest-neighbor (kdtree/nearest-neighbor tree %)]
            (when nearest-neighbor
              (assoc nearest-neighbor :base-point %)))
         (tool.hierarchy/snapping-bases db))))

(m/=> find-nearest-neighbor [:-> App [:maybe NearestNeighbor]])
(defn find-nearest-neighbor
  [db]
  (let [threshold (-> db :snap :threshold)
        nearest-neighbors (find-nearest-neighbors db)
        threshold (/ threshold (get-in db [:documents (:active-document db) :zoom]))
        nearest-neighbor (reduce
                          (fn [nearest-neighbor neighbor]
                            (if (< (:dist-squared neighbor)
                                   (:dist-squared nearest-neighbor))
                              neighbor
                              nearest-neighbor))
                          (first nearest-neighbors)
                          (rest nearest-neighbors))]
    (when (< (:dist-squared nearest-neighbor) (Math/pow threshold 2))
      nearest-neighbor)))

(m/=> update-nearest-neighbor [:-> App App])
(defn update-nearest-neighbor
  [db]
  (let [nearest-neighbor (find-nearest-neighbor db)]
    (cond-> db
      :always
      (dissoc :nearest-neighbor)

      (and (-> db :snap :active) nearest-neighbor)
      (assoc :nearest-neighbor nearest-neighbor))))

(m/=> nearest-delta [:-> App Vec2D])
(defn nearest-delta
  [db]
  (let [{:keys [point base-point]} (:nearest-neighbor db)]
    (mat/sub point base-point)))

(defn snap-with
  [db f & more]
  (let [db (update-nearest-neighbor db)]
    (if (:nearest-neighbor db)
      (apply f db (nearest-delta db) more)
      db)))
