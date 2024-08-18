(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as element.h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.math :as math]
   [renderer.utils.pointer :as pointer]))

(defn pan-by
  [{:keys [active-document] :as db} offset]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (update-in db
               [:documents active-document :pan]
               mat/add
               (mat/div offset zoom))))

(defn recenter-to-dom-rect
  [{dom-rect :dom-rect :as db} updated-dom-rect]
  (let [offset (select-keys (merge-with - dom-rect updated-dom-rect)
                            [:width :height])]
    (pan-by db (mat/div [(:width offset) (:height offset)] 2))))

(defn zoom-in-position
  [{:keys [active-document] :as db} factor pos]
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

(defn adjust-pointer-pos
  [{:keys [active-document] :as db} pos]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])]
    (pointer/adjust-position zoom pan pos)))

(defn zoom-in-pointer-position
  [{:keys [adjusted-pointer-pos] :as db} factor]
  (zoom-in-position db factor adjusted-pointer-pos))

(defn zoom-by
  [{:keys [active-document dom-rect] :as db} factor]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} dom-rect
        position (mat/add pan (mat/div [width height] 2 zoom))]
    (zoom-in-position db factor position)))

(defn pan-to-bounds
  [{:keys [active-document dom-rect] :as db} bounds]
  (let [zoom (get-in db [:documents active-document :zoom])
        [x1 y1] bounds
        pan (mat/add
             (mat/div
              (mat/sub
               (bounds/->dimensions bounds)
               (mat/div [(:width dom-rect) (:height dom-rect)]
                        zoom))
              2)
             [x1 y1])]
    (assoc-in db [:documents active-document :pan] pan)))

(defn focus-bounds
  ([db focus-type]
   (focus-bounds db focus-type (or (element.h/bounds db)
                                   (element/bounds (element.h/root-children db)))))
  ([{:keys [active-document dom-rect] :as db} focus-type bounds]
   (let [zoom (-> db :documents active-document :zoom)
         [width height] (bounds/->dimensions bounds)
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
  (pan-by db [(calc-pan-offset x offset-x width)
              (calc-pan-offset y offset-y height)]))
