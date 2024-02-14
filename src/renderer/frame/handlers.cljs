(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as element.h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.math :as math]
   [renderer.utils.element :as element]))

(defn pan
  [{:keys [active-document] :as db} offset]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (update-in db
               [:documents active-document :pan]
               mat/add
               (mat/div offset zoom))))

(defn zoom-in-position
  [{:keys [active-document] :as db} factor pos]
  (let [zoom (get-in db [:documents active-document :zoom])
        updated-zoom (math/clamp (* zoom factor) 0.01 100)
        updated-factor (/ updated-zoom zoom)
        pan (get-in db [:documents active-document :pan])
        updated-pan (mat/sub (mat/div pan updated-factor)
                             (mat/sub (mat/div pos updated-factor)
                                      pos))]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(defn adjust-pointer-pos
  [zoom pan pointer-pos]
  (-> pointer-pos
      (mat/div zoom)
      (mat/add pan)))

(defn adjusted-pointer-pos
  [{:keys [active-document] :as db} pointer-pos]
  (let [{:keys [zoom pan snap?]} (get-in db [:documents active-document])
        adjusted-pointer-pos (adjust-pointer-pos zoom pan pointer-pos)]
    (cond->> adjusted-pointer-pos
      snap? (mapv Math/round)))) ; FIXME: Pixel snapping.

(defn zoom-in-pointer-position
  [{:keys [pointer-pos] :as db} factor]
  (zoom-in-position db factor (adjusted-pointer-pos db pointer-pos)))

(defn zoom
  [{:keys [active-document content-rect] :as db} factor]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} content-rect
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-in-position db factor position)))

(defn pan-to-bounds
  [{:keys [active-document content-rect] :as db} bounds]
  (let [zoom (get-in db [:documents active-document :zoom])
        [x1 y1] bounds
        pan (mat/add
             (mat/div
              (mat/sub
               (bounds/->dimensions bounds)
               (mat/div [(:width content-rect) (:height content-rect)]
                        zoom))
              2)
             [x1 y1])]
    (assoc-in db [:documents active-document :pan] pan)))

(defn pan-to-element
  [db key]
  (let [element (element.h/element db key)
        elements (element.h/elements db)]
    (pan-to-bounds db (element/adjusted-bounds element elements))))

(defn focus-selection
  [{:keys [active-document content-rect] :as db} zoom]
  (if-let [bounds (element.h/bounds db)]
    (let [[width height] (bounds/->dimensions bounds)
          width-ratio (/ (:width content-rect) width)
          height-ratio (/ (:height content-rect) height)]
      (-> db
          (assoc-in [:documents active-document :zoom]
                    (case zoom
                      :original 1
                      :fit (min width-ratio height-ratio)
                      :fill (max width-ratio height-ratio)))
          (pan-to-bounds bounds)))
    (pan-to-element db (-> db (element.h/element :canvas) :children first))))

(defn calc-pan-offset
  [position offset size]
  (let [threshold 100
        step 15]
    (cond
      (and (< position threshold)
           (< position offset))
      (- step)

      (and (> position (- size threshold))
           (> position offset))
      step

      :else 0)))

(defn pan-out-of-canvas
  [db {:keys [width height]} [x y] [offset-x offset-y]]
  (pan db [(calc-pan-offset x offset-x width)
           (calc-pan-offset y offset-y height)]))

(defn recenter-to-content-rect
  [{content-rect :content-rect :as db} updated-content-rect]
  (let [offset (select-keys (merge-with - content-rect updated-content-rect)
                            [:width :height])]
    (pan db (mat/div [(:width offset) (:height offset)] 2))))
