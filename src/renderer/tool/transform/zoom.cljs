(ns renderer.tool.transform.zoom
  (:require
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.handlers :as handlers]
   [renderer.tool.base :as tool]
   [renderer.tool.overlay :as overlay]
   [renderer.utils.pointer :as pointer]))

(derive :zoom ::tool/tool)

(defmethod tool/properties :zoom
  []
  {:icon "magnifier"})

(defmethod tool/activate :zoom
  [db]
  (-> db
      (assoc :cursor "zoom-in")
      (handlers/set-message
       [:div
        [:div "Click or select an area to zoom in."]
        [:div "Hold " [:strong "Shift"] " to zoom out."]])))

(defmethod tool/key-down :zoom
  [db e]
  (cond-> db
    (pointer/shift? e)
    (assoc :cursor "zoom-out")))

(defmethod tool/key-up :zoom
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (assoc :cursor "zoom-in")))

(defmethod tool/drag-start :zoom
  [db]
  (assoc db :cursor "default"))

(defmethod tool/drag :zoom
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos active-document] :as db}]
  (element.h/set-temp db (overlay/select-box
                          adjusted-pointer-pos
                          adjusted-pointer-offset
                          (get-in db [:documents active-document :zoom]))))

(defmethod tool/drag-end :zoom
  [{:keys [active-document
           dom-rect
           adjusted-pointer-offset
           adjusted-pointer-pos
           zoom-sensitivity] :as db} e]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        width (abs (- pos-x offset-x))
        height (abs (- pos-y offset-y))
        width-ratio (/ (:width dom-rect) width)
        height-ratio (/ (:height dom-rect) height)
        current-zoom (get-in db [:documents active-document :zoom])
        furute-zoom (min width-ratio height-ratio)]
    (-> db
        element.h/clear-temp
        (assoc :cursor (if (pointer/shift? e) "zoom-out" "zoom-in"))
        (frame.h/zoom-by (if (pointer/shift? e)
                           zoom-sensitivity
                           (/ furute-zoom current-zoom)))
        (frame.h/pan-to-bounds [pos-x pos-y offset-x offset-y]))))

(defmethod tool/pointer-up :zoom
  [db e]
  (let [factor (if (pointer/shift? e)
                 (:zoom-sensitivity db)
                 (/ 1 (:zoom-sensitivity db)))]
    (frame.h/zoom-in-pointer-position db factor)))
