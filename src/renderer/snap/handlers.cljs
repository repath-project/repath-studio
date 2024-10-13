(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree :as kdtree]
   [malli.experimental :as mx]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.snap.db :refer [SnapOption]]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.utils.element :as utils.el]))

(mx/defn toggle-option
  [db, option :- SnapOption]
  (if (contains? (-> db :snap :options) option)
    (update-in db [:snap :options] disj option)
    (update-in db [:snap :options] conj option)))

(defn base-points
  [db]
  (let [elements (vals (element.h/elements db))
        selected-visible (filter #(and (:visible %) (:selected %)) elements)]
    (when (-> db :snap :active)
      (cond
        (and (contains? #{:move :clone} (:state db)) (seq selected-visible))
        (reduce (fn [points element]
                  (apply conj points (utils.el/snapping-points element (-> db :snap :options))))
                [] selected-visible)

        (contains? #{:edit :scale} (:state db))
        [(mat/add [(-> db :clicked-element :x) (-> db :clicked-element :y)]
                  (mat/sub (:adjusted-pointer-pos db)
                           (:adjusted-pointer-offset db)))]

        :else
        [(:adjusted-pointer-pos db)]))))

(defn find-nearest-neighbors
  [db]
  (let [tree @(rf/subscribe [::snap.s/in-viewport-tree])] ; FIXME: Subscription in event.
    (map #(let [nearest-neighbor (kdtree/nearest-neighbor tree %)]
            (when nearest-neighbor
              (assoc nearest-neighbor :base-point %))) (base-points db))))

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

(defn update-nearest-neighbor
  [db]
  (let [nearest-neighbor (find-nearest-neighbor db)]
    (cond-> db
      :always
      (update :snap dissoc :nearest-neighbor)

      (and (-> db :snap :active) nearest-neighbor)
      (assoc-in [:snap :nearest-neighbor] nearest-neighbor))))

(defn nearest-neighbor
  [db]
  (get-in db [:snap :nearest-neighbor]))

(defn nearest-delta
  [db]
  (let [{:keys [point base-point]} (nearest-neighbor db)]
    (mat/sub point base-point)))

(mx/defn snap-with
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
