(ns renderer.snap.handlers
  (:require
   [clojure.core.matrix :as mat]
   [kdtree :as kdtree]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.snap.subs :as-alias snap.s]
   [renderer.utils.element :as utils.el]))

(defn base-points
  [{:keys [snap
           adjusted-pointer-pos
           adjusted-pointer-offset
           clicked-element
           state] :as db}]
  (let [elements (element.h/elements db)
        selected-visible (filter #(and (:visible? %)
                                       (:selected? %)) (vals elements))]
    (when (:enabled? snap)
      (cond
        (and (contains? #{:move :clone} state) (seq selected-visible))
        (reduce (fn [points element]
                  (apply conj points (utils.el/snapping-points element (:options snap))))
                [] selected-visible)

        (contains? #{:edit :scale} state)
        [(mat/add [(:x clicked-element) (:y clicked-element)]
                  (mat/sub adjusted-pointer-pos
                           adjusted-pointer-offset))]

        :else
        [adjusted-pointer-pos]))))

(defn nearest-neighbors
  [db]
  (let [tree @(rf/subscribe [::snap.s/in-viewport-tree])] ; FIXME: Subscription in event.
    (map #(let [nearest-neighbor (kdtree/nearest-neighbor tree %)]
            (when nearest-neighbor
              (assoc nearest-neighbor :base-point %))) (base-points db))))

(defn nearest-neighbor
  [{:keys [active-document snap] :as db}]
  (let [threshold (:threshold snap)
        nearest-neighbors (nearest-neighbors db)
        threshold (/ threshold (get-in db [:documents active-document :zoom]))
        nearest-neighbor (reduce
                          (fn [nearest-neighbor neighbor]
                            (if (< (:dist-squared neighbor)
                                   (:dist-squared nearest-neighbor))
                              neighbor
                              nearest-neighbor))
                          (first nearest-neighbors)
                          (rest nearest-neighbors))]
    (when (< (:dist-squared nearest-neighbor) threshold)
      nearest-neighbor)))

(defn snap-to-offset
  [db f offset more]
  (apply f db offset more))

(defn snap
  [{:keys [snap] :as db} f & more]
  (let [{:keys [point base-point] :as nearest-neighbor} (nearest-neighbor db)]
    (cond-> db
      :always
      (update :snap dissoc :nearest-neighbor)

      (and (:enabled? snap) nearest-neighbor)
      (-> (assoc-in [:snap :nearest-neighbor] nearest-neighbor)
          (snap-to-offset f (mat/sub point base-point) more)))))
