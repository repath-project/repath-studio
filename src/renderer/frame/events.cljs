(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as h]))

(rf/reg-event-db
 ::resize
 (fn [db [_ dom-rect]]
   (assoc db :dom-rect dom-rect)))

(rf/reg-event-db
 ::center
 (fn [db [_]]
   (h/pan-to-element db)))

(rf/reg-event-db
 ::focus-selection
 (fn [db [_ zoom]]
   (h/focus-selection db zoom)))

(rf/reg-event-fx
 ::set-zoom
 (fn [{:keys [db]} [_ zoom]]
   (let [current-zoom (get-in db [:documents (:active-document db) :zoom])]
     {:db (h/zoom-by db (/ zoom current-zoom))
      :focus nil})))

(rf/reg-event-fx
 ::zoom-in
 (fn [{:keys [db]} [_ _]]
   {:db (h/zoom-by db (/ 1 (:zoom-sensitivity db)))
    :focus nil}))

(rf/reg-event-fx
 ::zoom-out
 (fn [{:keys [db]} [_ _]]
   {:db (h/zoom-by db (:zoom-sensitivity db))
    :focus nil}))

(rf/reg-event-db
 ::pan-to-bounds
 (fn [db [_ bounds]]
   (cond-> db
     (= (:state db) :default)
     (h/pan-to-bounds bounds))))

(rf/reg-event-fx
 ::pan-to-element
 (fn [{:keys [db]} [_ k]]
   {:fx
    (let [{:keys [dom-rect active-document]} db
          element (element.h/element db k)
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
                            :dispatch [::pan-to-bounds bounds]}])))}))
