(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [malli.experimental :as mx]
   [renderer.element.handlers :as element.h]
   [renderer.utils.bounds :as utils.bounds :refer [bounds]]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math :refer [vec2d]]
   [renderer.utils.pointer :as pointer]))

(mx/defn pan-by
  ([{:keys [active-document] :as db}, offset :- vec2d]
   (pan-by db offset active-document))
  ([db offset id]
   (let [zoom (get-in db [:documents id :zoom])]
     (update-in db [:documents id :pan] mat/add (mat/div offset zoom)))))

(mx/defn recenter-to-dom-rect
  [{:keys [dom-rect] :as db}, updated-dom-rect]
  (let [offset (select-keys (merge-with - dom-rect updated-dom-rect) [:width :height])]
    (if-not (-> db :window :focused?)
      db
      (reduce #(pan-by %1 (mat/div [(:width offset) (:height offset)] 2) %2) db (:document-tabs db)))))

(mx/defn zoom-in-position
  [{:keys [active-document] :as db}, factor :- number?, pos :- vec2d]
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
  [{:keys [active-document] :as db}, pos :- vec2d]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])]
    (pointer/adjust-position zoom pan pos)))

(mx/defn zoom-in-pointer-position
  [{:keys [adjusted-pointer-pos] :as db} factor :- number?]
  (zoom-in-position db factor adjusted-pointer-pos))

(mx/defn zoom-by
  [{:keys [active-document dom-rect] :as db}, factor :- number?]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} dom-rect
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-in-position db factor position)))

(mx/defn pan-to-bounds
  [{:keys [active-document dom-rect] :as db}, bounds :- bounds]
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

(mx/defn calc-pan-offset
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
  [db, {:keys [width height]}, [x y] :- vec2d, [offset-x offset-y] :- vec2d]
  (pan-by db [(calc-pan-offset x offset-x width)
              (calc-pan-offset y offset-y height)]))
