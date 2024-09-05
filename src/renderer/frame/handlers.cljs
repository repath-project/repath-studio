(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.app.db :refer [DomRect]]
   [renderer.element.handlers :as element.h]
   [renderer.utils.bounds :as utils.bounds :refer [Bounds]]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math :refer [Vec2D]]
   [renderer.utils.pointer :as pointer]))

(mx/defn pan-by
  ([{:keys [active-document] :as db}, offset :- Vec2D]
   (pan-by db offset active-document))
  ([db, offset :- Vec2D, id  :- uuid?]
   (let [zoom (get-in db [:documents id :zoom])]
     (update-in db [:documents id :pan] mat/add (mat/div offset zoom)))))

(mx/defn recenter-to-dom-rect
  [{:keys [dom-rect] :as db}, updated-dom-rect :- DomRect]
  (let [offset (select-keys (merge-with - dom-rect updated-dom-rect) [:width :height])]
    (if-not (-> db :window :focused?)
      db
      (reduce #(pan-by %1 (mat/div [(:width offset) (:height offset)] 2) %2) db (:document-tabs db)))))

(mx/defn zoom-in-place
  [{:keys [active-document] :as db}, factor :- number?, pos :- Vec2D]
  (let [zoom (get-in db [:documents active-document :zoom])
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
  [{:keys [active-document] :as db}, pos :- Vec2D]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])]
    (pointer/adjust-position zoom pan pos)))

(mx/defn zoom-at-pointer
  [{:keys [adjusted-pointer-pos] :as db}, factor :- number?]
  (zoom-in-place db factor adjusted-pointer-pos))

(mx/defn zoom-by
  [{:keys [active-document dom-rect] :as db}, factor :- number?]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} dom-rect
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-in-place db factor position)))

(mx/defn pan-to-bounds
  [{:keys [active-document dom-rect] :as db}, bounds :- Bounds]
  (let [zoom (get-in db [:documents active-document :zoom])
        [x1 y1] bounds
        pan (mat/add
             (mat/div
              (mat/sub
               (utils.bounds/->dimensions bounds)
               (mat/div [(:width dom-rect) (:height dom-rect)]
                        zoom))
              2)
             [x1 y1])]
    (assoc-in db [:documents active-document :pan] pan)))

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
  [db,
   {:keys [width height]} :- DomRect,
   [x y] :- Vec2D,
   [offset-x offset-y] :- Vec2D]
  (pan-by db [(axis-offset x offset-x width)
              (axis-offset y offset-y height)]))
