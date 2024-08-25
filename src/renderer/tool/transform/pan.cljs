(ns renderer.tool.transform.pan
  (:require
   [clojure.core.matrix :as mat]
   [renderer.app.effects :as-alias app.fx]
   [renderer.app.handlers :as app.h]
   [renderer.frame.handlers :as frame.h]
   [renderer.tool.base :as tool]))

(derive :pan ::tool/tool)

(defmethod tool/properties :pan
  []
  {:icon "hand"})

(defmethod tool/activate :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tool/activate :pan
  [db]
  (-> db
      (app.h/set-state :default)
      (app.h/set-cursor "grab")
      (app.h/set-message "Click and drag to pan.")))

(defmethod tool/pointer-up :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tool/pointer-down :pan
  [db]
  (assoc db :cursor "grabbing"))

(defmethod tool/drag :pan
  [db e]
  (frame.h/pan-by db (mat/sub (:pointer-pos db) (:pointer-pos e))))

(defmethod tool/drag-end :pan
  [db]
  (-> db
      (assoc :cursor "grab")
      (app.h/add-fx [::app.fx/local-storage-persist nil])))
