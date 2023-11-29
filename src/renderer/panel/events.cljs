(ns renderer.panel.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.utils.local-storage :as local-storage]))

(rf/reg-event-db
 :panel/toggle
 [local-storage/persist
  (rf/path :panel)]
 (fn [db [_ key]]
   (update-in db [key :visible?] not)))

(rf/reg-event-db
 :panel/set-drag
 (fn [db [_ key direction]]
   (-> db
       (assoc-in [:panel-state :drag] key)
       (assoc-in [:panel-state :drag-direction] direction))))

(rf/reg-event-db
 :panel/clear-drag
 (fn [db [_]]
   (update db :panel-state dissoc :drag :pointer-pos)))

(rf/reg-event-db
 :panel/on-drag
 local-storage/persist
 (fn [db [_ evt]]
   (when-let [key (-> db :panel-state :drag)]
     (let [pointer-pos [(.-clientX evt) (.-clientY evt)]
           min-width 300
           max-width 600
           previous-pointer-pos (-> db :panel-state :pointer-pos)
           current-size (-> db :panel key :size)
           offset (when previous-pointer-pos (mat/sub previous-pointer-pos pointer-pos))
           direction (-> db :panel-state :drag-direction)
           updated-size ((if (contains? #{:right :bottom} direction) + -)
                         current-size
                         ((if (contains? #{:left :right} direction) first second) offset))]
       (cond-> db
         (or (< min-width updated-size max-width)
             (not (-> db :panel-state :pointer-pos)))
         (assoc-in [:panel-state :pointer-pos] pointer-pos)

         (and (not (-> db :panel key :visible?)) (> updated-size current-size))
         (assoc-in [:panel key :visible?] true)

         (and key previous-pointer-pos)
         (assoc-in [:panel key :size] (cond (< updated-size min-width) min-width
                                            (> updated-size max-width) max-width
                                            :else updated-size)))))))
