(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.document.events :as-alias document.e]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as h]
   [renderer.snap.handlers :as snap.h]))

(rf/reg-event-db
 ::resize
 [persist]
 (fn [db [_ dom-rect]]
   (-> db
       (h/recenter-to-dom-rect dom-rect)
       (assoc :dom-rect dom-rect)
       (snap.h/update-viewport-tree))))

(rf/reg-event-db
 ::focus-selection
 [persist]
 (fn [db [_ focus-type]]
   (-> (h/focus-bbox db focus-type)
       (snap.h/update-viewport-tree))))

(rf/reg-event-db
 ::set-zoom
 [persist]
 (fn [db [_ zoom]]
   (let [current-zoom (get-in db [:documents (:active-document db) :zoom])]
     (h/zoom-in-place db (/ zoom current-zoom)))))

(rf/reg-event-db
 ::zoom-in
 [persist]
 (fn [db [_]]
   (h/zoom-in-place db (/ 1 (:zoom-sensitivity db)))))

(rf/reg-event-db
 ::zoom-out
 [persist]
 (fn [db [_]]
   (h/zoom-in-place db (:zoom-sensitivity db))))

(rf/reg-event-db
 ::pan-to-element
 [persist]
 (fn [db [_ id]]
   (let [element (element.h/entity db id)
         el-bbox (:bbox element)
         viewbox (h/viewbox db)
         diff (mat/sub el-bbox viewbox)
         bbox (mat/add viewbox diff)]
     (h/pan-to-bbox db bbox))))
