(ns renderer.timeline.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]))

(defn svg-elements
  []
  (when-let [canvas (dom/canvas-element)]
    (.querySelectorAll canvas "svg")))

(rf/reg-fx
 ::set-current-time
 (fn [time]
   (doall (map #(.setCurrentTime % time) (svg-elements)))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (doall (map #(.pauseAnimations %) (svg-elements)))))

(rf/reg-event-db
 :timeline/pause
 (fn [db _]
   (assoc-in db [:timeline :paused?] true)))

(rf/reg-event-db
 :timeline/play
 (fn [db _]
   (assoc-in db [:timeline :paused?] false)))

(rf/reg-event-db
 :timeline/set-grid-snap
 (fn [db [_ state]]
   (assoc-in db [:timeline :grid-snap?] state)))

(rf/reg-event-db
 :timeline/set-guide-snap
 (fn [db [_ state]]
   (assoc-in db [:timeline :guide-snap?] state)))

(rf/reg-event-db
 :timeline/toggle-replay
 (fn [db _]
   (update-in db [:timeline :replay?] not)))

(rf/reg-event-db
 :timeline/set-speed
 (fn [db [_ speed]]
   (assoc-in db [:timeline :speed] speed)))

(rf/reg-event-fx
 :timeline/set-time
 (fn [{:keys [db]} [_ time]]
   {:db (assoc-in db [:timeline :time] time)
    ::set-current-time time
    ::pause-animations nil}))
