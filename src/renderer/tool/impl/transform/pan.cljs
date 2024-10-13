(ns renderer.tool.impl.transform.pan
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :pan ::hierarchy/tool)

(defmethod hierarchy/properties :pan
  []
  {:icon "hand"})

(defmethod hierarchy/activate :pan
  [db]
  (app.h/set-cursor db "grab"))

(defmethod hierarchy/help [:pan :default]
  []
  "Click and drag to pan.")

(defmethod hierarchy/activate :pan
  [db]
  (-> db
      (app.h/set-state :default)
      (app.h/set-cursor "grab")))

(defmethod hierarchy/pointer-up :pan
  [db]
  (app.h/set-cursor db "grab"))

(defmethod hierarchy/pointer-down :pan
  [db]
  (app.h/set-cursor db "grabbing"))

(defmethod hierarchy/drag :pan
  [db e]
  (frame.h/pan-by db (mat/sub (:pointer-pos db) (:pointer-pos e))))

(defmethod hierarchy/drag-end :pan
  [db _e]
  (-> db
      (app.h/set-cursor "grab")
      (app.h/add-fx [:dispatch [::app.e/persist]])))
