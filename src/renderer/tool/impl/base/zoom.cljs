(ns renderer.tool.impl.base.zoom
  (:require
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.app.effects :as-alias app.effects]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.frame.handlers :as frame.handlers]
   [renderer.snap.handlers :as snap.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.svg :as utils.svg]))

(derive :zoom ::tool.hierarchy/tool)

(defonce element (reagent/atom nil))

(rf/reg-fx
 ::update
 (fn [value]
   (reset! element value)))

(defmethod tool.hierarchy/properties :zoom
  []
  {:icon "magnifier"
   :label (t [::label "Zoom"])})

(defmethod tool.hierarchy/help [:zoom :idle]
  []
  [:<>
   (t [::zoom-in "Click or select an area to zoom in."])
   (t [::zoom-out "Hold %1 to zoom out."] [[:span.shortcut-key "⇧"]])])

(defmethod tool.hierarchy/on-activate :zoom
  [db]
  (tool.handlers/set-cursor db "zoom-in"))

(defmethod tool.hierarchy/on-deactivate :zoom
  [db]
  (tool.handlers/add-fx db [::update nil]))

(defmethod tool.hierarchy/on-key-down :zoom
  [db e]
  (cond-> db
    (:shift-key e)
    (tool.handlers/set-cursor "zoom-out")))

(defmethod tool.hierarchy/on-key-up :zoom
  [db e]
  (cond-> db
    (not (:shift-key e))
    (tool.handlers/set-cursor "zoom-in")))

(defmethod tool.hierarchy/on-drag :zoom
  [db _e]
  (tool.handlers/add-fx db [::update (utils.svg/select-box db)]))

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
        zoom (min width-ratio height-ratio)
        factor (if (:shift-key e) (:zoom-sensitivity db) (/ zoom current-zoom))
        cursor (if (:shift-key e) "zoom-out" "zoom-in")]
    (-> db
        (tool.handlers/add-fx [::update nil])
        (tool.handlers/set-cursor cursor)
        (frame.handlers/zoom-in-place factor)
        (frame.handlers/pan-to-bbox [x y offset-x offset-y])
        (snap.handlers/update-viewport-tree)
        (tool.handlers/add-fx [::app.effects/persist]))))

(defmethod tool.hierarchy/on-pointer-up :zoom
  [db e]
  (let [factor (cond->> (:zoom-sensitivity db) (not (:shift-key e)) (/ 1))]
    (-> (frame.handlers/zoom-at-pointer db factor)
        (snap.handlers/update-viewport-tree)
        (tool.handlers/add-fx [::app.effects/persist]))))

(defmethod tool.hierarchy/render :zoom
  []
  [element.hierarchy/render @element])
