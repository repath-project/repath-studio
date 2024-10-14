(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.app.db :refer [DomRect]]
   [renderer.document.db :refer [ZoomFactor]]
   [renderer.element.handlers :as element.h]
   [renderer.utils.bounds :as utils.bounds :refer [Bounds]]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math :refer [Vec2D]]
   [renderer.utils.pointer :as pointer]))

(mx/defn viewbox
  [zoom :- ZoomFactor, pan :- Vec2D, dom-rect :- DomRect]
  (let [{:keys [width height]} dom-rect
        [x y] pan
        [width height] (mat/div [width height] zoom)]
    [x y width height]))

(mx/defn pan-by
  ([db, offset :- Vec2D]
   (pan-by db offset (:active-document db)))
  ([db, offset :- Vec2D, id  :- uuid?]
   (let [zoom (get-in db [:documents id :zoom])]
     (update-in db [:documents id :pan] mat/add (mat/div offset zoom)))))

(mx/defn recenter-to-dom-rect
  [db, updated-dom-rect :- DomRect]
  (let [offset (-> (merge-with - (:dom-rect db) updated-dom-rect)
                   (select-keys [:width :height]))]
    (if-not (-> db :window :focused)
      db
      (reduce #(pan-by %1 (mat/div [(:width offset) (:height offset)] 2) %2) db (:document-tabs db)))))

(mx/defn zoom-in-place
  [db, factor :- number?, pos :- Vec2D]
  (let [active-document (:active-document db)
        zoom (get-in db [:documents active-document :zoom])
        updated-zoom (math/clamp (* zoom factor) 0.01 100)
        updated-factor (/ updated-zoom zoom)
        current-pan (get-in db [:documents active-document :pan])
        updated-pan (mat/sub (mat/div current-pan updated-factor)
                             (mat/sub (mat/div pos updated-factor)
                                      pos))]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(mx/defn adjust-pointer-pos
  [db, pos :- Vec2D]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])]
    (pointer/adjust-position zoom pan pos)))

(mx/defn zoom-at-pointer
  [db, factor :- number?]
  (zoom-in-place db factor (:adjusted-pointer-pos db)))

(mx/defn zoom-by
  [db, factor :- number?]
  (let [{:keys [zoom pan]} (get-in db [:documents (:active-document db)])
        {:keys [width height]} (:dom-rect db)
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-in-place db factor position)))

(mx/defn pan-to-bounds
  [db, bounds :- Bounds]
  (let [zoom (get-in db [:documents (:active-document db) :zoom])
        rect-dimentions [(-> db :dom-rect :width) (-> db :dom-rect :height)]
        [x1 y1] bounds
        pan (-> (utils.bounds/->dimensions bounds)
                (mat/sub (mat/div rect-dimentions zoom))
                (mat/div 2)
                (mat/add [x1 y1]))]
    (assoc-in db [:documents (:active-document db) :pan] pan)))

(mx/defn focus-bounds
  ([db, focus-type :- [:enum :original :fit :fill]]
   (cond-> db
     (:active-document db)
     (focus-bounds focus-type (or (element.h/bounds db)
                                  (element/united-bounds (element.h/root-children db))))))
  ([{:keys [active-document dom-rect] :as db} focus-type bounds]
   (let [[width height] (utils.bounds/->dimensions bounds)
         width-ratio (/ (:width dom-rect) width)
         height-ratio (/ (:height dom-rect) height)
         min-zoom (min width-ratio height-ratio)]
     (-> db
         (assoc-in [:documents active-document :zoom]
                   (case focus-type
                     :original (min (* min-zoom 0.9) 1)
                     :fit min-zoom
                     :fill (max width-ratio height-ratio)))
         (pan-to-bounds bounds)))))

(mx/defn axis-offset :- number?
  [position :- number?, offset :- number?, size :- number?]
  (let [threshold 50
        step 15]
    (cond
      (and (< position threshold)
           (< position offset))
      (- step)

      (and (> position (- size threshold))
           (> position offset))
      step

      :else 0)))

(mx/defn pan-out-of-canvas
  [db, dom-rect :- DomRect, [x y] :- Vec2D, [offset-x offset-y] :- Vec2D]
  (pan-by db [(axis-offset x offset-x (:width dom-rect))
              (axis-offset y offset-y (:height dom-rect))]))
