(ns renderer.frame.events
  (:require
   [re-frame.core :as rf]
   [renderer.frame.handlers :as handlers]
   [clojure.core.matrix :as matrix]
   [renderer.element.handlers :as el]
   [renderer.utils.units :as units]))

(rf/reg-event-db
 :frame/resize
 (fn [{content-rect :content-rect :as db} [_ updated-content-rect]]
   (let [offset (-> (merge-with - content-rect updated-content-rect)
                    (select-keys [:width :height]))
         pan (matrix/div [(:width offset) (:height offset)] 2)]
     (-> db
         (assoc :content-rect updated-content-rect)
         (handlers/pan pan)))))

(rf/reg-event-db
 :pan
 (fn [db [_ [x y]]]
   (handlers/pan db [x y])))

(rf/reg-event-db
 :pan-to-element
 (fn [db [_ key]]
   (handlers/pan-to-element db key)))

(rf/reg-event-db
 :pan-to-active-page
 (fn [{:keys [active-document content-rect] :as db}  [_ zoom]]
   (let [active-page (get-in db [:documents active-document :active-page])
         {:keys [width height]} (:attrs (el/get-element db active-page))
         [width height] (map units/unit->px [width height])
         width-ratio (/ (:width content-rect) width)
         height-ratio (/ (:height content-rect) height)]
     (-> db
         (assoc-in [:documents active-document :zoom]
                   (case zoom
                     :original 1
                     :fit (min width-ratio height-ratio)
                     :fill (max width-ratio height-ratio)))
         (handlers/pan-to-element active-page)))))

(rf/reg-event-db
 :zoom
 (fn [db [_ factor]]
   (handlers/zoom db factor)))

#_(rf/reg-event-db
   :set-zoom
   (fn [{active-document :active-document :as db} [_ zoom]]
     (let [current-zoom (get-in db [:documents active-document :zoom])]
       (handlers/zoom db (/ zoom current-zoom)))))

(rf/reg-event-db
 :zoom-in
 (fn [db [_ _]]
   (handlers/zoom db (/ 1 (:zoom-factor db)))))

(rf/reg-event-db
 :zoom-out
 (fn [db [_ _]]
   (handlers/zoom db (:zoom-factor db))))
