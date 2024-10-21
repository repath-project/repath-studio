(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree :as kdtree]
   [malli.core :as m]
   [re-frame.core :as rf]
   [renderer.app.db :refer [App]]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.snap.db :refer [SnapOption NearestNeighbor]]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.utils.element :as utils.el]
   [renderer.utils.math :refer [Vec2D]]))

(m/=> toggle-option [:-> App SnapOption App])
(defn toggle-option
  [db option]
  (if (contains? (-> db :snap :options) option)
    (update-in db [:snap :options] disj option)
    (update-in db [:snap :options] conj option)))

(m/=> base-points [:-> App [:vector Vec2D]])
(defn base-points
  [db]
  (let [elements (vals (element.h/entities db))
        selected-visible (filter #(and (:visible %) (:selected %)) elements)]
    (when (-> db :snap :active)
      (cond
        (and (contains? #{:translate :clone} (:state db)) (seq selected-visible))
        (reduce (fn [points element]
                  (apply conj points (utils.el/snapping-points element (-> db :snap :options))))
                [] selected-visible)

        (contains? #{:edit :scale} (:state db))
        [(mat/add [(-> db :clicked-element :x) (-> db :clicked-element :y)]
                  (app.h/pointer-delta db))]

        :else
        [(:adjusted-pointer-pos db)]))))

(m/=> find-nearest-neighbors [:-> App [:sequential NearestNeighbor]])
(defn find-nearest-neighbors
  [db]
  (let [tree @(rf/subscribe [::snap.s/in-viewport-tree])] ; FIXME: Subscription in event.
    (map #(let [nearest-neighbor (kdtree/nearest-neighbor tree %)]
            (when nearest-neighbor
              (assoc nearest-neighbor :base-point %))) (base-points db))))

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
      (update :snap dissoc :nearest-neighbor)

      (and (-> db :snap :active) nearest-neighbor)
      (assoc-in [:snap :nearest-neighbor] nearest-neighbor))))

(m/=> nearest-neighbor [:-> App NearestNeighbor])
(defn nearest-neighbor
  [db]
  (get-in db [:snap :nearest-neighbor]))

(m/=> nearest-delta [:-> App Vec2D])
(defn nearest-delta
  [db]
  (let [{:keys [point base-point]} (nearest-neighbor db)]
    (mat/sub point base-point)))

(defn snap-with
  ([db f]
   (let [db (update-nearest-neighbor db)]
     (cond-> db
       (nearest-neighbor db)
       (f (nearest-delta db)))))
  ([db f arg1]
   (let [db (update-nearest-neighbor db)]
     (cond-> db
       (nearest-neighbor db)
       (f (nearest-delta db) arg1))))
  ([db f arg1 arg2]
   (let [db (update-nearest-neighbor db)]
     (cond-> db
       (nearest-neighbor db)
       (f (nearest-delta db) arg1 arg2))))
  ([db f arg1 arg2 arg3]
   (let [db (update-nearest-neighbor db)]
     (cond-> db
       (nearest-neighbor db)
       (f (nearest-delta db) arg1 arg2 arg3))))
  ([db f arg1 arg2 arg3 & more]
   (let [db (update-nearest-neighbor db)]
     (cond-> db
       (nearest-neighbor db)
       (apply f (nearest-delta db) arg1 arg2 arg3 more)))))
