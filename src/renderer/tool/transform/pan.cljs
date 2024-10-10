(ns renderer.tool.transform.pan
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :pan ::tool.hierarchy/tool)

(defmethod tool.hierarchy/properties :pan
  []
  {:icon "hand"})

(defmethod tool.hierarchy/activate :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tool.hierarchy/help [:pan :default]
  []
  "Click and drag to pan.")

(defmethod tool.hierarchy/activate :pan
  [db]
  (-> db
      (app.h/set-state :default)
      (app.h/set-cursor "grab")))

(defmethod tool.hierarchy/pointer-up :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tool.hierarchy/pointer-down :pan
  [db]
  (assoc db :cursor "grabbing"))

(defmethod tool.hierarchy/drag :pan
  [db e]
  (frame.h/pan-by db (mat/sub (:pointer-pos db) (:pointer-pos e))))

(defmethod tool.hierarchy/drag-end :pan
  [db _e]
  (-> db
      (app.h/set-cursor "grab")
      (app.h/add-fx [:dispatch [::app.e/persist]])))
