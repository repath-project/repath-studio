(ns repath.studio.window.events
  (:require
   [re-frame.core :as rf]
   [clojure.core.matrix :as matrix]))

(rf/reg-event-db
 :window/set-bitmap-data
 (rf/path :window)
 (fn [db [_ data]]
   (assoc db
          :bitmap (.-bitmap data)
          :size (js->clj (.-size data) :keywordize-keys true))))

(rf/reg-event-db
 :window/set-maximized?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :maximized? state)))

(rf/reg-event-db
 :window/set-fullscreen?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :fullscreen? state)))

(rf/reg-event-db
 :window/set-minimized?
 (rf/path :window)
 (fn [db [_ state]]
   (assoc db :minimized? state)))

(rf/reg-event-db
 :window/toggle-sidebar
 (rf/path :window)
 (fn [db [_ key]]
   (update-in db [key :visible?] not)))

(rf/reg-event-db
 :window/toggle-header
 (rf/path :window)
 (fn [db [_]]
   (update db :header? not)))

(rf/reg-event-db
 :window/toggle-elements-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :elements-collapsed? not)))

(rf/reg-event-db
 :window/toggle-pages-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :pages-collapsed? not)))

(rf/reg-event-db
 :window/toggle-symbols-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :symbols-collapsed? not)))

(rf/reg-event-db
 :window/toggle-repl-history-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :repl-history-collapsed? not)))

(rf/reg-event-db
 :window/toggle-defs-collapsed
 (rf/path :window)
 (fn [db [_]]
   (update db :defs-collapsed? not)))

(rf/reg-event-db
 :window/set-drag
 (fn [db [_ key direction]]
   (-> db
       (assoc-in [:window :drag] key)
       (assoc-in [:window :drag-direction] direction))))

(rf/reg-event-db
 :window/clear-drag
 (fn [db [_]]
   (update db :window dissoc :drag :mouse-pos)))

(rf/reg-event-db
 :window/on-drag
 (fn [db [_ evt]]
   (let [mouse-pos [(.-clientX evt) (.-clientY evt)]
         min-width 300
         key (-> db :window :drag)
         previous-mouse-pos (-> db :window :mouse-pos)
         current-size (-> db :window key :size)
         offset (when previous-mouse-pos (matrix/sub previous-mouse-pos mouse-pos))
         direction (-> db :window :drag-direction )
         updated-size ((if (contains? #{:right :bottom} direction) + -) current-size ((if (contains? #{:left :right} direction) first second) offset))]
     (cond-> db
       (or (> updated-size min-width) (not (-> db :window :mouse-pos))) (assoc-in [:window :mouse-pos] mouse-pos)
       (and (not (-> db :window key :visible?)) (> updated-size current-size)) (assoc-in [:window key :visible?] true)
       (and key previous-mouse-pos) (assoc-in [:window key :size] (if (> updated-size min-width) updated-size min-width))))))