(ns repath.studio.canvas-frame.events
  (:require
   [re-frame.core :as rf]
   [repath.studio.canvas-frame.handlers :as h]
   [repath.studio.tools.base :as tools]
   [clojure.core.matrix :as matrix]
   [repath.studio.elements.handlers :as el]
   [repath.studio.handlers :as handlers]
   [repath.studio.units :as units]))

(rf/reg-event-db
 :canvas/resize
 (fn [{content-rect :content-rect :as db} [_ updated-content-rect]]
   (let [offset (-> (merge-with - content-rect updated-content-rect)
                    (select-keys [:width :height]))
         pan (matrix/div [(:width offset) (:height offset)] 2)]
     (-> db
         (assoc :content-rect updated-content-rect)
         (h/pan pan)))))

(rf/reg-event-db
 :pan
 (fn [db [_ [x y]]]
   (h/pan db [x y])))

(rf/reg-event-db
 :pan-to-element
 (fn [db [_ key]]
   (h/pan-to-element db key)))

(rf/reg-event-db
 :pan-to-active-page
 (fn [{:keys [active-document content-rect] :as db}  [_ zoom]]
   (let [active-page (get-in db [:documents active-document :active-page])
         {:keys [width height]} (:attrs (el/element db active-page))
         [width height] (map units/unit->px [width height])
         width-ratio (/ (:width content-rect) width)
         height-ratio (/ (:height content-rect) height)]
     (-> db
         (assoc-in [:documents active-document :zoom] (case zoom
                                                        :original 1
                                                        :fit (min width-ratio height-ratio)
                                                        :fill (max width-ratio height-ratio)))
         (h/pan-to-element active-page)))))


(rf/reg-event-db
 :zoom-in-mouse-position
 (fn [db [_ factor]]
   (h/zoom-in-mouse-position db factor)))

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
   (h/zoom db (/ 1 (:zoom-factor db)))))

(rf/reg-event-db
 :zoom-out
 (fn [db [_ _]]
   (h/zoom db (:zoom-factor db))))

(rf/reg-event-db
 :mouse-event
 (fn [{:keys [mouse-offset active-document tool] :as db} [_ event]]
   (let [{:keys [fill stroke zoom selected-keys]} (get-in db [:documents active-document])
         {:keys [type mouse-pos modifiers delta button element]} event
         adjusted-mouse-pos (h/adjusted-mouse-pos db mouse-pos)
         tool-data {:mouse-pos mouse-pos
                    :adjusted-mouse-pos adjusted-mouse-pos
                    :is-element-selected? (contains? selected-keys (:key element))
                    :fill fill
                    :zoom zoom
                    :stroke stroke}]
     (case type
       :mousemove
       (-> (if mouse-offset
             (tools/drag db event element tool-data)
             (tools/mouse-move db event element tool-data))
           (assoc :mouse-pos mouse-pos)
           (assoc :mouse-over-canvas? true)
           (assoc :adjusted-mouse-pos adjusted-mouse-pos))

       :mousedown
       (if-not mouse-offset
         (cond-> db
           (= button 1) (assoc :cached-tool tool)
           (= button 1) (handlers/set-tool :pan)
           :always (assoc :mouse-offset mouse-pos)
           :always (assoc :adjusted-mouse-offset adjusted-mouse-pos))
         db)

       :mouseup
       (cond-> (if (not= mouse-pos mouse-offset)
                 (tools/drag-end db event element tool-data)
                 (tools/click db event element tool-data))
         :always (dissoc :mouse-offset)
         (:cached-tool db) (handlers/set-tool (:cached-tool db))
         :always (dissoc :cached-tool))

       :wheel
       (if (some modifiers [:ctrl :alt])
         (let [factor (if (pos? (second delta)) (:zoom-factor db) (/ 1 (:zoom-factor db)))]
           (h/zoom-in-mouse-position db factor))
         (h/pan db delta))
       db))))
