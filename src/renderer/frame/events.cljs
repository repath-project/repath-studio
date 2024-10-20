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
 ::pan-to-element
 [persist]
 (fn [db [_ id]]
   (let [element (element.h/entity db id)
         el-bounds (:bounds element)
         zoom (get-in db [:documents (:active-document db) :zoom])
         pan (get-in db [:documents (:active-document db) :pan])
         viewbox (h/viewbox zoom pan (:dom-rect db))
         diff (mat/sub el-bounds viewbox)
         bounds (mat/add viewbox diff)]
     (h/pan-to-bounds db bounds))))
