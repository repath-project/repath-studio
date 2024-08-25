(ns renderer.frame.events
  (:require
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.events :as-alias document.e]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as h]
   [renderer.utils.local-storage :as local-storage]))

(def focus-canvas
  (rf/->interceptor
   :id ::focus-canvas
   :after (fn [context] (assoc-in context [:effects ::app.fx/focus] nil))))

(rf/reg-event-db
 ::resize
 local-storage/persist
 (fn [db [_ dom-rect]]
   (-> db
       #_(h/recenter-to-dom-rect dom-rect)
       (assoc :dom-rect dom-rect))))

(rf/reg-event-db
 ::focus-selection
 local-storage/persist
 (fn [db [_ focus-type]]
   (h/focus-bounds db focus-type)))

(rf/reg-event-db
 ::set-zoom
 local-storage/persist
 (fn [db [_ zoom]]
   (let [current-zoom (get-in db [:documents (:active-document db) :zoom])]
     (h/zoom-by db (/ zoom current-zoom)))))

(rf/reg-event-db
 ::zoom-in
 local-storage/persist
 (fn [db [_ _]]
   (h/zoom-by db (/ 1 (:zoom-sensitivity db)))))

(rf/reg-event-db
 ::zoom-out
 local-storage/persist
 (fn [db [_ _]]
   (h/zoom-by db (:zoom-sensitivity db))))

(rf/reg-event-db
 ::pan-to-bounds
 local-storage/persist
 (fn [db [_ bounds]]
   (cond-> db
     (= (:state db) :default)
     (h/pan-to-bounds bounds))))

(rf/reg-event-fx
 ::pan-to-element
 (fn [{:keys [db]} [_ id]]
   {:fx
    (let [{:keys [dom-rect active-document]} db
          element (element.h/element db id)
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
