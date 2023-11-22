(ns renderer.tools.pan
  (:require
   [clojure.core.matrix :as mat]
   [renderer.frame.handlers :as frame]
   [renderer.tools.base :as tools]))

(derive :pan ::tools/transform)

(defmethod tools/properties :pan
  []
  {:icon "hand"})

(defmethod tools/activate :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tools/mouse-up :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tools/mouse-down :pan
  [db]
  (assoc db :cursor "grabbing"))

(defmethod tools/drag :pan
  [db e _]
  (frame/pan db (mat/sub (:mouse-pos db) (:mouse-pos e))))

(defmethod tools/drag-end :pan
  [db]
  (assoc db :cursor "grab"))
