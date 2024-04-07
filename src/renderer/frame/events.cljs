(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as h]))

(rf/reg-event-db
 :frame/resize
 (fn [db [_ dom-rect]]
   (-> db
       (h/recenter-to-dom-rect dom-rect)
       (assoc :dom-rect dom-rect))))

(rf/reg-event-db
 :frame/center
 (fn [db [_]]
   (h/pan-to-element db)))

(rf/reg-event-db
 :frame/focus-selection
 (fn [db [_ zoom]]
   (h/focus-selection db zoom)))

(rf/reg-event-db
 :frame/set-zoom
 (fn [{active-document :active-document :as db} [_ zoom]]
   (let [current-zoom (get-in db [:documents active-document :zoom])]
     (h/zoom db (/ zoom current-zoom)))))

(rf/reg-event-db
 :frame/zoom-in
 (fn [db [_ _]]
   (h/zoom db (/ 1 (:zoom-sensitivity db)))))

(rf/reg-event-db
 :frame/zoom-out
 (fn [db [_ _]]
   (h/zoom db (:zoom-sensitivity db))))

(rf/reg-event-db
 :frame/pan-to-bounds
 (fn [db [_ bounds]]
   (cond-> db
     (= (:state db) :default)
     (h/pan-to-bounds bounds))))

(rf/reg-event-fx
 :frame/pan-to-element
 (fn [{:keys [db]} [_ key]]
   {:fx
    (let [{:keys [dom-rect active-document]} db
          element (element.h/element db key)
          el-bounds (:bounds element)
          zoom (get-in db [:documents active-document :zoom])
          {:keys [width height]} dom-rect
          [x y] (get-in db [:documents active-document :pan])
          [width height] (mat/div [width height] zoom)
          viewbox [x y (+ x width) (+ y height)] ; TODO: Convert to flow.
          diff (mat/sub el-bounds viewbox)
          frames 30]
      (for [i (range (inc frames))]
        (let [bounds (mat/add viewbox (mat/mul diff (/ i frames)))]
          [:dispatch-later {:ms (* i 10) ; TODO: Easing and canceling.
                            :dispatch [:frame/pan-to-bounds bounds]}])))}))
