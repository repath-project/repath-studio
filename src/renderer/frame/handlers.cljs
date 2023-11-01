(ns renderer.frame.handlers
  (:require
   [renderer.tools.base :as tools]
   [renderer.element.handlers :as el]
   [renderer.element.utils :as el-utils]
   [clojure.core.matrix :as matrix]
   [goog.math]))

(defn pan
  [{:keys [active-document] :as db} offset]
  (let [zoom (get-in db [:documents active-document :zoom])]
    (update-in db
               [:documents active-document :pan]
               matrix/add
               (matrix/div offset zoom))))

(defn zoom-in-position
  [{:keys [active-document] :as db} factor pos]
  (let [zoom (get-in db [:documents active-document :zoom])
        updated-zoom (goog.math.clamp (* zoom factor) 0.01 100)
        updated-factor (/ updated-zoom zoom)
        pan (get-in db [:documents active-document :pan])
        updated-pan (matrix/sub (matrix/div pan updated-factor)
                                (matrix/sub (matrix/div pos updated-factor)
                                            pos))]
    (-> db
        (assoc-in [:documents active-document :zoom] updated-zoom)
        (assoc-in [:documents active-document :pan] updated-pan))))

(defn adjust-mouse-pos
  [zoom pan mouse-pos]
  (-> mouse-pos
      (matrix/div zoom)
      (matrix/add pan)))

(defn adjusted-mouse-pos
  [{:keys [active-document] :as db} mouse-pos]
  (let [{:keys [zoom pan snap?]} (get-in db [:documents active-document])
        adjusted-mouse-pos (adjust-mouse-pos zoom pan mouse-pos)]
    (if snap?
      (mapv Math/round (adjust-mouse-pos zoom pan mouse-pos))
      adjusted-mouse-pos)))

(defn zoom-in-mouse-position
  [{:keys [mouse-pos] :as db} factor]
  (zoom-in-position db factor (adjusted-mouse-pos db mouse-pos)))

(defn zoom
  [{:keys [active-document content-rect] :as db} factor]
  (let [{:keys [zoom pan]} (get-in db [:documents active-document])
        {:keys [width height]} content-rect
        position (matrix/add pan (matrix/div [width height] 2 zoom))]
    (zoom-in-position db factor position)))

(defn pan-to-bounds
  [{:keys [active-document content-rect] :as db} bounds]
  (let [zoom (get-in db [:documents active-document :zoom])
        [x1 y1 x2 y2] bounds
        dimensions (matrix/sub [x2 y2] [x1 y1])
        pan (matrix/add
             (matrix/div
              (matrix/sub
               dimensions
               (matrix/div [(:width content-rect) (:height content-rect)]
                           zoom))
              2)
             [x1 y1])]
    (assoc-in db [:documents active-document :pan] pan)))

(defn pan-to-element
  [db key]
  (let [element (el/get-element db key)
        elements (el/elements db)
        parrent-page-attrs (:attrs (el-utils/parent-page elements element))
        db (pan-to-bounds db (tools/bounds element elements))]
    (if (not (el/page? element))
      (pan db [(:x parrent-page-attrs)
               (:y parrent-page-attrs)])
      db)))

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