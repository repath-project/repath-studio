(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx :refer [persist]]
   [renderer.app.events]
   [renderer.document.events :as-alias document.e]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as h]))

(rf/reg-event-db
 ::resize
 [persist]
 (fn [db [_ dom-rect]]
   (-> db
       (h/recenter-to-dom-rect dom-rect)
       (assoc :dom-rect dom-rect))))

(rf/reg-event-db
 ::focus-selection
 [persist]
 (fn [db [_ focus-type]]
   (h/focus-bounds db focus-type)))

(rf/reg-event-db
 ::set-zoom
 [persist]
 (fn [db [_ zoom]]
   (let [current-zoom (get-in db [:documents (:active-document db) :zoom])]
     (h/zoom-by db (/ zoom current-zoom)))))

(rf/reg-event-db
 ::zoom-in
 [persist]
 (fn [db [_ _]]
   (h/zoom-by db (/ 1 (:zoom-sensitivity db)))))

(rf/reg-event-db
 ::zoom-out
 [persist]
 (fn [db [_ _]]
   (h/zoom-by db (:zoom-sensitivity db))))

(rf/reg-event-db
 ::pan-to-bounds
 [persist]
 (fn [db [_ bounds]]
   (cond-> db
     (= (:state db) :idle)
     (h/pan-to-bounds bounds))))

(rf/reg-event-fx
 ::pan-to-element
 (fn [{:keys [db]} [_ id]]
   {:fx
    (let [element (element.h/element db id)
          el-bounds (:bounds element)
          zoom (get-in db [:documents (:active-document db) :zoom])
          pan (get-in db [:documents (:active-document db) :pan])
          viewbox (h/viewbox zoom pan (:dom-rect db))
          diff (mat/sub el-bounds viewbox)
          frames 30]
      (for [i (range (inc frames))]
        (let [bounds (mat/add viewbox (mat/mul diff (/ i frames)))]
          [:dispatch-later {:ms (* i 10) ; TODO: Easing and canceling.
                            :dispatch [::pan-to-bounds bounds]}])))}))
