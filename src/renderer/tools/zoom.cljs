(ns renderer.tools.zoom
  (:require
   [renderer.frame.handlers :as frame]
   [renderer.element.handlers :as elements]
   [renderer.overlay :as overlay]
   [renderer.tools.base :as tools]
   [renderer.handlers :as handlers]))

(derive :zoom ::tools/transform)

(defmethod tools/properties :zoom
  []
  {:icon "magnifier"})

(defmethod tools/activate :zoom
  [db]
  (-> db
      (assoc :cursor "zoom-in")
      (handlers/set-message
       [:div
        [:div "Click or select an area to zoom in."]
        [:div "Hold " [:strong "Shift"] " to zoom out."]])))

(defmethod tools/key-down :zoom
  [db event]
  (if (contains? (:modifiers event) :shift)
    (assoc db :cursor "zoom-out")
    db))

(defmethod tools/key-up :zoom
  [db event]
  (if (not (contains? (:modifiers event) :shift))
    (assoc db :cursor "zoom-in")
    db))

(defmethod tools/drag-start :zoom
  [db]
  (assoc db :cursor "default"))

(defmethod tools/drag :zoom
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos active-document] :as db}]
  (elements/set-temp db (overlay/select-box
                         adjusted-mouse-pos
                         adjusted-mouse-offset
                         (get-in db [:documents active-document :zoom]))))

(defmethod tools/drag-end :zoom
  [{:keys [active-document
           content-rect
           adjusted-mouse-offset
           adjusted-mouse-pos
           zoom-factor] :as db} event]
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
        (frame/zoom (if (contains? (:modifiers event) :shift)
                      zoom-factor
                      (/ furute-zoom current-zoom)))
        (frame/pan-to-bounds [pos-x pos-y offset-x offset-y]))))

(defmethod tools/mouse-up :zoom
  [db event]
  (let [factor (if (contains? (:modifiers event) :shift)
                 (:zoom-factor db)
                 (/ 1 (:zoom-factor db)))]
    (frame/zoom-in-mouse-position db factor)))