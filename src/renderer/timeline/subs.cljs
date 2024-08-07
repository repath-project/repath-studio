(ns renderer.timeline.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.tool.base :as tool]))

(rf/reg-sub
 ::animations
 :<- [::document.s/elements]
 (fn [elements]
   (filter #(contains? (descendants ::tool/animation) (:tag %))
           (vals elements))))

(defn effect-id
  [el]
  (str "effect" (name (:key el))))

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
                :name (str/join " " [(name (:tag el)) (:attributeName attrs)])
                :start start
                :end end
                :effectId (effect-id el)}]}))

(rf/reg-sub
 ::rows
 :<- [::animations]
 (fn [animations]
   (->> animations
        (mapv animation->timeline-row)
        clj->js)))

(rf/reg-sub
 ::end
 :<- [::animations]
 (fn [animations]
   (reduce #(max (js/parseInt (-> %2 :attrs :end)) %1) 0 animations)))

(defn animation->effect
  [el]
  {:id (effect-id el)})

(rf/reg-sub
 ::effects
 :<- [::animations]
 (fn [animations]
   (->> animations
        (reduce #(assoc %1 (effect-id %2) (animation->effect %2)) {})
        clj->js)))

(defn pad-2
  [n]
  (-> n str js/parseInt str (.padStart 2 "0")))

(rf/reg-sub
 ::time
 (fn [db _]
   (-> db :timeline :time)))

(rf/reg-sub
 ::time-formatted
 :<- [::time]
 (fn [time]
   (let [min (-> time (/ 60) pad-2)
         sec (-> time (rem 60) pad-2)
         ms (-> time (rem 1) (* 100) pad-2 (str/replace "0." ""))]
     (str min ":"  sec ":" ms))))

(rf/reg-sub
 ::paused?
 (fn [db _]
   (-> db :timeline :paused?)))

(rf/reg-sub
 ::grid-snap?
 (fn [db _]
   (-> db :timeline :grid-snap?)))

(rf/reg-sub
 ::guide-snap?
 (fn [db _]
   (-> db :timeline :guide-snap?)))

(rf/reg-sub
 ::replay?
 (fn [db _]
   (-> db :timeline :replay?)))

(rf/reg-sub
 ::speed
 (fn [db _]
   (-> db :timeline :speed)))
