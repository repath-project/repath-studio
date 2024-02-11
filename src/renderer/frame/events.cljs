(ns renderer.frame.events
  (:require
   [re-frame.core :as rf]
   [renderer.element.handlers :as el]
   [renderer.frame.handlers :as h]
   [renderer.utils.bounds :as bounds]))

(rf/reg-event-db
 :frame/resize
 (fn [db [_ updated-content-rect]]
   (-> db
       ;; This works, but lets keep it simple for now.
       #_(h/recenter-to-content-rect updated-content-rect)
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
 :pan-to-selected
 (fn [{:keys [active-document content-rect] :as db}  [_ zoom]]
   (if-let [bounds (el/bounds db)]
     (let [[width height] (bounds/->dimensions bounds)
           width-ratio (/ (:width content-rect) width)
           height-ratio (/ (:height content-rect) height)]
       (-> db
           (assoc-in [:documents active-document :zoom]
                     (case zoom
                       :original 1
                       :fit (min width-ratio height-ratio)
                       :fill (max width-ratio height-ratio)))
           (h/pan-to-bounds bounds))) db)))

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
