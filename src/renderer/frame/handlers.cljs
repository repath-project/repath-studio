(ns renderer.frame.handlers
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.handlers :as element.h]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.math :as math]
   [renderer.utils.pointer :as pointer]))

(defn pan-by
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

(defn pan-to-element
  ([db]
   (pan-to-element db (-> db element.h/root :children first)))
  ([db k]
   (let [element (element.h/element db k)
         el-bounds (:bounds element)]
     (cond-> db
       el-bounds
       (pan-to-bounds el-bounds)))))

(defn focus-selection
  [{:keys [active-document dom-rect] :as db} zoom-type]
  (if-let [bounds (element.h/bounds db)]
    (let [[width height] (bounds/->dimensions bounds)
          width-ratio (/ (:width dom-rect) width)
          height-ratio (/ (:height dom-rect) height)]
      (-> db
          (assoc-in [:documents active-document :zoom]
                    (case zoom-type
                      :original 1
                      :fit (min width-ratio height-ratio)
                      :fill (max width-ratio height-ratio)))
          (pan-to-bounds bounds)))
    db))

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

(defn recenter-to-dom-rect
  [{dom-rect :dom-rect :as db} updated-dom-rect]
  (let [offset (select-keys (merge-with - dom-rect updated-dom-rect)
                            [:width :height])]
    (pan-by db (mat/div [(:width offset) (:height offset)] 2))))
