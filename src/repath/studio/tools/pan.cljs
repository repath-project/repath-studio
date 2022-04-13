(ns repath.studio.tools.pan
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.canvas-frame.handlers :as canvas-frame]
            [clojure.core.matrix :as matrix]))

(derive :pan ::tools/transform)

(defmethod tools/properties :pan [] {:icon "hand"})

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
  [db event _]
  (canvas-frame/pan db (matrix/sub (:mouse-pos db) (:mouse-pos event))))

(defmethod tools/drag-end :pan
  [db]
  (assoc db :cursor "grab"))


