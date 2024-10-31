(ns renderer.tool.impl.base.pan
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.effects :as-alias app.fx]
   [renderer.frame.handlers :as frame.h]
   [renderer.snap.handlers :as snap.h]
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
      (snap.h/update-viewbox-tree)
      (h/add-fx [::app.fx/persist])))
