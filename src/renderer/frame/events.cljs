(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as matrix]
   [re-frame.core :as rf]
   [renderer.app.events :refer [persist]]
   [renderer.document.events :as-alias document.events]
   [renderer.element.handlers :as element.handlers]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]))

(rf/reg-event-db
 ::resize
 [persist]
 (fn [db [_ dom-rect]]
   (-> db
       (frame.handlers/recenter-to-dom-rect dom-rect)
       (assoc :dom-rect dom-rect)
       (snap.handlers/update-viewport-tree))))

(rf/reg-event-db
 ::focus-selection
 [persist]
 (fn [db [_ focus-type]]
   (-> (frame.handlers/focus-bbox db focus-type)
       (snap.handlers/update-viewport-tree))))

(rf/reg-event-db
 ::set-zoom
 [persist]
 (fn [db [_ zoom]]
   (let [current-zoom (get-in db [:documents (:active-document db) :zoom])]
     (frame.handlers/zoom-in-place db (/ zoom current-zoom)))))

(rf/reg-event-db
 ::zoom-in
 [persist]
 (fn [db [_]]
   (frame.handlers/zoom-in-place db (/ 1 (:zoom-sensitivity db)))))

(rf/reg-event-db
 ::zoom-out
 [persist]
 (fn [db [_]]
   (frame.handlers/zoom-in-place db (:zoom-sensitivity db))))

(rf/reg-event-db
 ::pan-to-element
 [persist]
 (fn [db [_ id]]
   (let [element (element.handlers/entity db id)
         el-bbox (:bbox element)
         viewbox (frame.handlers/viewbox db)
         diff (matrix/sub el-bbox viewbox)
         bbox (matrix/add viewbox diff)]
     (frame.handlers/pan-to-bbox db bbox))))
