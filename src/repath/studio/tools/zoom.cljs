(ns repath.studio.tools.zoom
  (:require [repath.studio.canvas-frame.handlers :as canvas-frame]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.styles :as styles]
            [repath.studio.tools.base :as tools]))

(derive :zoom ::tools/transform)

(defmethod tools/properties :zoom [] {:icon "magnifier"})

(defmethod tools/activate :zoom
  [db _ _ _]
  (assoc db :cursor "zoom-in"))

(defmethod tools/drag :zoom
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos active-document] :as db}]
  (let [zoom (get-in db [:documents active-document :zoom])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:key    :select
               :x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (Math/abs (- pos-x offset-x))
               :height (Math/abs (- pos-y offset-y))
               :fill   styles/accent
               :fill-opacity ".25"
               :stroke styles/accent
               :stroke-opacity ".5"
               :stroke-width (/ 1 zoom)}]
    (-> db
        (assoc :state :select)
        (elements/set-temp {:type :rect :attrs attrs}))))

(defmethod tools/drag-end :zoom
  [{:keys [active-document content-rect adjusted-mouse-offset adjusted-mouse-pos] :as db} event]
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
          (canvas-frame/zoom (if (contains? (:modifiers event) :shift) (:zoom-factor db) (/ furute-zoom current-zoom)))
          (canvas-frame/pan-to-bounds [pos-x pos-y offset-x offset-y]))))

(defmethod tools/mouse-up :zoom
  [db event]
  (let [factor (if (contains? (:modifiers event) :shift) (:zoom-factor db) (/ 1 (:zoom-factor db)))]
    (canvas-frame/zoom-in-mouse-position db factor)))