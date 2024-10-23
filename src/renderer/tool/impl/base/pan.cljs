(ns renderer.tool.impl.base.pan
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.events :as-alias app.e]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :pan ::hierarchy/tool)

(defmethod hierarchy/properties :pan
  []
  {:icon "hand"})

(defmethod hierarchy/activate :pan
  [db]
  (h/set-cursor db "grab"))

(defmethod hierarchy/help [:pan :idle]
  []
  "Click and drag to pan.")

(defmethod hierarchy/activate :pan
  [db]
  (-> db
      (h/set-state :idle)
      (h/set-cursor "grab")))

(defmethod hierarchy/pointer-up :pan
  [db]
  (h/set-cursor db "grab"))

(defmethod hierarchy/pointer-down :pan
  [db]
  (h/set-cursor db "grabbing"))

(defmethod hierarchy/drag :pan
  [db e]
  (frame.h/pan-by db (mat/sub (:pointer-pos db) (:pointer-pos e))))

(defmethod hierarchy/drag-end :pan
  [db _e]
  (-> db
      (h/set-cursor "grab")
      (h/add-fx [:dispatch [::app.e/persist]])))
