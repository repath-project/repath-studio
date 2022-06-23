(ns repath.studio.tools.zoom
  (:require [repath.studio.canvas-frame.handlers :as canvas-frame]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.tools.base :as tools]))

(derive :zoom ::tools/transform)

(defmethod tools/properties :zoom [] {:icon "magnifier"})

(defmethod tools/activate :zoom
  [db]
  (assoc db :cursor "zoom-in"))

(defmethod tools/drag-start :zoom
  [db]
  (assoc db :cursor "default"))

(defmethod tools/drag :zoom
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos active-document] :as db}]
  (elements/set-temp db (element-views/select-box adjusted-mouse-pos adjusted-mouse-offset (get-in db [:documents active-document :zoom]))))

(defmethod tools/drag-end :zoom
  [{:keys [active-document content-rect adjusted-mouse-offset adjusted-mouse-pos] :as db} event]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        width  (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        width-ratio (/ (:width content-rect) width)
        height-ratio (/ (:height content-rect) height)
        current-zoom (get-in db [:documents active-document :zoom])
        furute-zoom (min width-ratio height-ratio)]
      (-> db
          (elements/clear-temp)
          (assoc :cursor "zoom-in")
          (canvas-frame/zoom (if (contains? (:modifiers event) :shift) (:zoom-factor db) (/ furute-zoom current-zoom)))
          (canvas-frame/pan-to-bounds [pos-x pos-y offset-x offset-y]))))

(defmethod tools/mouse-up :zoom
  [db event]
  (let [factor (if (contains? (:modifiers event) :shift) (:zoom-factor db) (/ 1 (:zoom-factor db)))]
    (canvas-frame/zoom-in-mouse-position db factor)))