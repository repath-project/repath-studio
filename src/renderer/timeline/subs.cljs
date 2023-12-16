(ns renderer.timeline.subs
  (:require
   [re-frame.core :as rf]
   [renderer.tools.base :as tools]))

(defn effect-id
  [el]
  (str "effect" (:key el)))

(defn animation->timeline-row
  [{:keys [attrs] :as el}]
  (let [start (or (:begin attrs) 0)
        _dur (or (:dur attrs) 0)
        end (or (:end attrs) nil)]
    {:id (:key el)
     :selected (:selected? el)
     :actions [{:id (name (:key el))
                :selected (:selected? el)
                :disable (:locked? el)
                :movable (not (:locked? el))
                :start start
                :end end
                :effectId (effect-id el)}]}))

(defn animation->effect
  [{:keys [attrs] :as el}]
  {:id (effect-id el)
   :name (str (name (:tag el)) (:attributeName attrs))})

(rf/reg-sub
 :animations
 :<- [:document/elements]
 (fn [elements]
   (filter #(contains? (descendants ::tools/animation) (:tag %)) (vals elements))))

(rf/reg-sub
 :timeline/rows
 :<- [:animations]
 (fn [animations]
   (->> animations
        (mapv animation->timeline-row)
        clj->js)))

(rf/reg-sub
 :timeline/effects
 :<- [:animations]
 (fn [animations]
   (->> animations
        (reduce #(assoc %1 (effect-id %2) (animation->effect %2)) {})
        clj->js)))

(rf/reg-sub
 :timeline/time
 (fn [db _]
   (-> db :timeline :time)))

(rf/reg-sub
 :timeline/paused?
 (fn [db _]
   (-> db :timeline :paused?)))
