(ns renderer.tools.transform.zoom
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as frame]
   [renderer.handlers :as handlers]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.pointer :as pointer]))

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
  [db e]
  (if (pointer/shift? e)
    (assoc db :cursor "zoom-out")
    db))

(defmethod tools/key-up :zoom
  [db e]
  (if-not (pointer/shift? e)
    (assoc db :cursor "zoom-in")
    db))

(defmethod tools/drag-start :zoom
  [db]
  (assoc db :cursor "default"))

(defmethod tools/drag :zoom
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos active-document] :as db}]
  (element.h/set-temp db (overlay/select-box
                          adjusted-pointer-pos
                          adjusted-pointer-offset
                          (get-in db [:documents active-document :zoom]))))

(defmethod tools/drag-end :zoom
  [{:keys [active-document
           content-rect
           adjusted-pointer-offset
           adjusted-pointer-pos
           zoom-sensitivity] :as db} e]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        width-ratio (/ (:width content-rect) width)
        height-ratio (/ (:height content-rect) height)
        current-zoom (get-in db [:documents active-document :zoom])
        furute-zoom (min width-ratio height-ratio)]
    (-> db
        element.h/clear-temp
        (assoc :cursor "zoom-in")
        (frame/zoom (if (pointer/shift? e)
                      zoom-sensitivity
                      (/ furute-zoom current-zoom)))
        (frame/pan-to-bounds [pos-x pos-y offset-x offset-y]))))

(defmethod tools/pointer-up :zoom
  [db e]
  (let [factor (if (pointer/shift? e)
                 (:zoom-sensitivity db)
                 (/ 1 (:zoom-sensitivity db)))]
    (frame/zoom-in-pointer-position db factor)))
