(ns renderer.timeline.events
  (:require
   [re-frame.core :as rf]
   [renderer.utils.dom :as dom]))

(defn svg-elements
  []
  (-> (dom/canvas-element)
      (.querySelectorAll "svg")))

(rf/reg-fx
 ::set-current-time
 (fn [time]
   (doall (map #(.setCurrentTime % time) (svg-elements)))))

(rf/reg-fx
 ::pause-animations
 (fn []
   (doall (map #(.pauseAnimations %) (svg-elements)))))

(rf/reg-fx
 ::unpause-animations
 (fn []
   (doall (map #(.unpauseAnimations %) (svg-elements)))))

(rf/reg-event-fx
 :timeline/pause
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:timeline :paused?] true)
    ::pause-animations nil}))

(rf/reg-event-fx
 :timeline/play
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:timeline :paused?] false)}))

(rf/reg-event-fx
 :timeline/set-time
 (fn [{:keys [db]} [_ time]]
   {:db (assoc-in db [:timeline :time] time)
    ::set-current-time time}))

 (rf/reg-event-db
  :timeline/set-grid-snap
  (fn [db [_ state]]
    (assoc-in db [:timeline :grid-snap?] state)))

 (rf/reg-event-db
 :timeline/set-guide-snap
 (fn [db [_ state]]
   (assoc-in db [:timeline :guide-snap?] state)))
