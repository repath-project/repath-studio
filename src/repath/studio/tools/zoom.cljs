(ns repath.studio.tools.zoom
  (:require [repath.studio.canvas-frame.handlers :as canvas-frame]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]))

(derive :zoom ::tools/transform)

(defmethod tools/properties :zoom [] {:icon "magnifier"})

(defmethod tools/mouse-move :zoom
  [db event _ _]
  (assoc db :cursor (if (contains? (:modifiers event) :shift) "zoom-out" "zoom-in")))

(defmethod tools/drag :zoom
  [{:keys [adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos zoom]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:key    :select
               :x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))
               :fill   "transparent"
               :stroke "#000"
               :stroke-width (/ 1 zoom)}]
    (-> db
        (assoc :state :select)
        (elements/set-temp {:type :rect :attrs attrs}))))

(defmethod tools/drag-end :zoom
  [{:keys [active-document content-rect adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        width  (Math/abs (- pos-x offset-x))
        height (Math/abs (- pos-y offset-y))
        width-ratio (/ (:width content-rect) width)
        height-ratio (/ (:height content-rect) height)
        current-zoom (get-in db [:documents active-document :zoom])
        furute-zoom (min width-ratio height-ratio)]
    (-> db
        (elements/clear-temp)
        (assoc :state :default)
        (canvas-frame/zoom (/ furute-zoom current-zoom))
        (canvas-frame/pan-to-bounds [pos-x pos-y offset-x offset-y]))))

(defmethod tools/click :zoom
  [db event element tool-data]
  (let [zoom-out-factor 0.8
        zoom-in-factor 1.2
        factor (if (contains? (:modifiers event) :shift) zoom-out-factor zoom-in-factor)]
    (canvas-frame/zoom-in-mouse-position db factor)))