(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.utils.element :as utils.el]))

(defn base-points
  [{:keys [snap?
           adjusted-pointer-pos
           adjusted-pointer-offset
           clicked-element
           state] :as db}]
  (let [elements (element.h/elements db)
        selected-visible (filter #(and (:visible? %)
                                       (:selected? %)) (vals elements))]
    (when snap?
      (if (and (contains? #{:move :clone} state) (seq selected-visible))
        (reduce (fn [points element]
                  (apply conj points (utils.el/snapping-points element)))
                [] selected-visible)
        [(mat/add [(:x clicked-element) (:y clicked-element)]
                  (mat/sub adjusted-pointer-pos
                           adjusted-pointer-offset))]))))

(defn nearest-neighbors
  [db]
  (let [base-points (base-points db)
        tree @(rf/subscribe [:snap/in-viewport-tree])] ; FIXME: Subscription in event.
    (map #(let [nearest-neighbor (kdtree/nearest-neighbor tree %)]
            (when nearest-neighbor
              (assoc nearest-neighbor :base-point %))) base-points)))

(defn nearest-neighbor
  [{:keys [active-document snap-threshold] :as db}]
  (let [nearest-neighbors (nearest-neighbors db)
        snap-threshold (/ snap-threshold (-> db :documents active-document :zoom))
        nearest-neighbor (reduce
                          (fn [nearest-neighbor neighbor]
                            (if (< (:dist-squared neighbor)
                                   (:dist-squared nearest-neighbor))
                              neighbor
                              nearest-neighbor))
                          (first nearest-neighbors)
                          (rest nearest-neighbors))]
    (when (< (:dist-squared nearest-neighbor) snap-threshold)
      nearest-neighbor)))

(defn snap-to-offset
  [db f offset more]
  (apply f db offset more))

(defn snap
  [{:keys [snap?] :as db} f & more]
  (let [{:keys [point base-point] :as nearest-neighbor} (nearest-neighbor db)]
    (cond-> (dissoc db :snap)
      (and snap? nearest-neighbor)
      (-> (assoc :snap nearest-neighbor)
          (snap-to-offset f (mat/sub point base-point) more)))))
