(ns renderer.tool.impl.transform.zoom
  (:require
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.overlay :as overlay]
   [renderer.utils.pointer :as pointer]))

(derive :zoom ::hierarchy/tool)

(defmethod hierarchy/properties :zoom
  []
  {:icon "magnifier"})

(defmethod hierarchy/help [:zoom :default]
  []
  [:<>
   [:div "Click or select an area to zoom in."]
   [:div "Hold " [:span.shortcut-key "⇧"] " to zoom out."]])

(defmethod hierarchy/activate :zoom
  [db]
  (app.h/set-cursor db "zoom-in"))

(defmethod hierarchy/key-down :zoom
  [db e]
  (cond-> db
    (pointer/shift? e)
    (app.h/set-cursor "zoom-out")))

(defmethod hierarchy/key-up :zoom
  [db e]
  (cond-> db
    (not (pointer/shift? e))
    (app.h/set-cursor "zoom-in")))

(defmethod hierarchy/drag-start :zoom
  [db]
  (app.h/set-cursor db "default"))

(defmethod hierarchy/drag :zoom
  [db]
  (element.h/set-temp db (overlay/select-box db)))

(defmethod hierarchy/drag-end :zoom
  [db e]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        dom-rect (:dom-rect db)
        width-ratio (/ (:width dom-rect) width)
        height-ratio (/ (:height dom-rect) height)
        current-zoom (get-in db [:documents (:active-document db) :zoom])
        furute-zoom (min width-ratio height-ratio)]
    (-> db
        (element.h/dissoc-temp)
        (app.h/set-cursor (if (pointer/shift? e) "zoom-out" "zoom-in"))
        (frame.h/zoom-by (if (pointer/shift? e)
                           (:zoom-sensitivity db)
                           (/ furute-zoom current-zoom)))
        (frame.h/pan-to-bounds [x y offset-x offset-y])
        (app.h/add-fx [:dispatch [::app.e/persist]]))))

(defmethod hierarchy/pointer-up :zoom
  [db e]
  (let [factor (if (pointer/shift? e)
                 (:zoom-sensitivity db)
                 (/ 1 (:zoom-sensitivity db)))]
    (-> db
        (frame.h/zoom-at-pointer  factor)
        (app.h/add-fx [:dispatch [::app.e/persist]]))))
