(ns renderer.tool.impl.base.zoom
  (:require
   [renderer.app.effects :as-alias app.effects]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.pointer :as utils.pointer]
   [renderer.utils.svg :as utils.svg]))

(derive :zoom ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :zoom
  []
  {:icon "magnifier"})

(defmethod tool.hierarchy/help [:zoom :idle]
  []
  [:<>
   [:div "Click or select an area to zoom in."]
   [:div "Hold " [:span.shortcut-key "⇧"] " to zoom out."]])

(defmethod tool.hierarchy/on-activate :zoom
  [db]
  (tool.handlers/set-cursor db "zoom-in"))

(defmethod tool.hierarchy/on-key-down :zoom
  [db e]
  (cond-> db
    (utils.pointer/shift? e)
    (tool.handlers/set-cursor "zoom-out")))

(defmethod tool.hierarchy/on-key-up :zoom
  [db e]
  (cond-> db
    (not (utils.pointer/shift? e))
    (tool.handlers/set-cursor "zoom-in")))

(defmethod tool.hierarchy/on-drag-start :zoom
  [db _e]
  (tool.handlers/set-cursor db "default"))

(defmethod tool.hierarchy/on-drag :zoom
  [db _e]
  (tool.handlers/set-temp db (utils.svg/select-box db)))

(defmethod tool.hierarchy/on-drag-end :zoom
  [db e]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        dom-rect (:dom-rect db)
        width-ratio (/ (:width dom-rect) width)
        height-ratio (/ (:height dom-rect) height)
        current-zoom (get-in db [:documents (:active-document db) :zoom])
        zoom (min width-ratio height-ratio)]
    (-> (tool.handlers/dissoc-temp db)
        (tool.handlers/set-cursor (if (utils.pointer/shift? e) "zoom-out" "zoom-in"))
        (frame.handlers/zoom-in-place (if (utils.pointer/shift? e)
                                        (:zoom-sensitivity db)
                                        (/ zoom current-zoom)))
        (frame.handlers/pan-to-bbox [x y offset-x offset-y])
        (snap.handlers/update-viewport-tree)
        (tool.handlers/add-fx [::app.effects/persist]))))

(defmethod tool.hierarchy/on-pointer-up :zoom
  [db e]
  (let [factor (if (utils.pointer/shift? e)
                 (:zoom-sensitivity db)
                 (/ 1 (:zoom-sensitivity db)))]
    (-> (frame.handlers/zoom-at-pointer db factor)
        (snap.handlers/update-viewport-tree)
        (tool.handlers/add-fx [::app.effects/persist]))))
