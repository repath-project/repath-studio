(ns renderer.timeline.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.hierarchy :as element.hierarchy]))

(rf/reg-sub
 ::animations
 :<- [::document.s/elements]
 (fn [elements]
   (->> (vals elements)
        (filter #(contains? (descendants ::element.hierarchy/animation) (:tag %))))))

(defn effect-id
  [el]
  (str "effect" (:id el)))

(defn animation->timeline-row
  [{:keys [id attrs selected locked] :as el}]
  (let [start (or (:begin attrs) 0)
        _dur (or (:dur attrs) 0)
        end (or (:end attrs) nil)]
    {:id id
     :selected selected
     :actions [{:id (str id)
                :selected selected
                :disable locked
                :movable (not locked)
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
        (clj->js))))

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
        (clj->js))))

(defn pad-2
  [n]
  (-> n str js/parseInt str (.padStart 2 "0")))

(rf/reg-sub
 ::timeline
 :-> :timeline)

(rf/reg-sub
 ::time
 :<- [::timeline]
 :-> :time)

(rf/reg-sub
 ::time-formatted
 :<- [::time]
 (fn [t]
   (let [m (-> t (/ 60) pad-2)
         s (-> t (rem 60) pad-2)
         ms (-> t (rem 1) (* 100) pad-2 (str/replace "0." ""))]
     (str m ":"  s ":" ms))))

(rf/reg-sub
 ::paused?
 :<- [::timeline]
 :-> :paused?)

(rf/reg-sub
 ::grid-snap?
 :<- [::timeline]
 :-> :grid-snap?)

(rf/reg-sub
 ::guide-snap?
 :<- [::timeline]
 :-> :guide-snap?)

(rf/reg-sub
 ::replay?
 :<- [::timeline]
 :-> :replay?)

(rf/reg-sub
 ::speed
 :<- [::timeline]
 :-> :speed)
