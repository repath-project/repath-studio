(ns renderer.frame.events
  (:require
   [re-frame.core :as rf]
   [renderer.frame.handlers :as h]))

(rf/reg-event-db
 :frame/resize
 (fn [db [_ updated-content-rect]]
   (-> db
       (h/recenter-to-content-rect updated-content-rect)
       (assoc :content-rect updated-content-rect))))

(rf/reg-event-db
 :pan
 (fn [db [_ [x y]]]
   (h/pan db [x y])))

(rf/reg-event-db
 :pan-to-element
 (fn [db [_ key]]
   (h/pan-to-element db key)))

(rf/reg-event-db
 :focus-selection
 (fn [db [_ zoom]]
   (h/focus-selection db zoom)))

(rf/reg-event-db
 :zoom
 (fn [db [_ factor]]
   (h/zoom db factor)))

(rf/reg-event-db
 :set-zoom
 (fn [{active-document :active-document :as db} [_ zoom]]
   (let [current-zoom (get-in db [:documents active-document :zoom])]
     (h/zoom db (/ zoom current-zoom)))))

(rf/reg-event-db
 :zoom-in
 (fn [db [_ _]]
   (h/zoom db (/ 1 (:zoom-sensitivity db)))))

(rf/reg-event-db
 :zoom-out
 (fn [db [_ _]]
   (h/zoom db (:zoom-sensitivity db))))
