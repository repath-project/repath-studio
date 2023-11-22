(ns renderer.panel.events
  (:require
   [re-frame.core :as rf]
   [clojure.core.matrix :as mat]
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
   (update db :panel-state dissoc :drag :mouse-pos)))

(rf/reg-event-db
 :panel/on-drag
 local-storage/persist
 (fn [db [_ evt]]
   (when-let [key (-> db :panel-state :drag)]
     (let [mouse-pos [(.-clientX evt) (.-clientY evt)]
           min-width 300
           max-width 600
           previous-mouse-pos (-> db :panel-state :mouse-pos)
           current-size (-> db :panel key :size)
           offset (when previous-mouse-pos (mat/sub previous-mouse-pos mouse-pos))
           direction (-> db :panel-state :drag-direction)
           updated-size ((if (contains? #{:right :bottom} direction) + -)
                         current-size
                         ((if (contains? #{:left :right} direction) first second) offset))]
       (cond-> db
         (or (and (> updated-size min-width)
                  (< updated-size max-width))
             (not (-> db :panel-state :mouse-pos)))
         (assoc-in [:panel-state :mouse-pos] mouse-pos)

         (and (not (-> db :panel key :visible?)) (> updated-size current-size))
         (assoc-in [:panel key :visible?] true)

         (and key previous-mouse-pos)
         (assoc-in [:panel key :size] (cond (< updated-size min-width) min-width
                                            (> updated-size max-width) max-width
                                            :else updated-size)))))))
