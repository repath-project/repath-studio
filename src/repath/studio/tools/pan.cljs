(ns repath.studio.tools.pan
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.canvas-frame.handlers :as canvas-frame]
            [clojure.core.matrix :as matrix]))

(derive :pan ::tools/transform)

(defmethod tools/properties :pan [] {:icon "hand"})

(defmethod tools/activate :pan
  [db]
  (assoc db :cursor "grab"))

(defmethod tools/drag :pan
  [db event _]
  (-> db
      (assoc :cursor "grabbing")
      (canvas-frame/pan (matrix/sub (:mouse-pos db) (:mouse-pos event)))))

(defmethod tools/drag-end :pan
  [db]
  (-> db
      (assoc :cursor "grab")
      (assoc :state :default)))


