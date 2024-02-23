(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as h]
   [renderer.utils.element :as element]))

(rf/reg-event-db
 :frame/resize
 (fn [db [_ updated-content-rect]]
   (-> db
       (h/recenter-to-content-rect updated-content-rect)
       (assoc :content-rect updated-content-rect))))

(rf/reg-event-db
 :center
 (fn [db [_]]
   (h/pan-to-element db)))

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

(rf/reg-event-db
 :pan-to-bounds
 (fn [db [_ bounds]]
   (cond-> db
     (= (:state db) :default)
     (h/pan-to-bounds bounds))))

(rf/reg-event-fx
 :pan-to-element
 (fn [{:keys [db]} [_ key]]
   {:fx
    (let [{:keys [content-rect active-document]} db
          element (element.h/element db key)
          elements (element.h/elements db)
          el-bounds (element/adjusted-bounds element elements)
          zoom (get-in db [:documents active-document :zoom])
          {:keys [width height]} content-rect
          [x y] (get-in db [:documents active-document :pan])
          [width height] (mat/div [width height] zoom)
          viewbox [x y (+ x width) (+ y height)] ; TODO: Convert to flow.
          diff (mat/sub el-bounds viewbox)
          frames 30]
      (for [i (range (inc frames))]
        (let [bounds (mat/add viewbox (mat/mul diff (/ i frames)))]
          [:dispatch-later {:ms (* i 10) ; TODO: Easing and canceling.
                            :dispatch [:pan-to-bounds bounds]}])))}))
