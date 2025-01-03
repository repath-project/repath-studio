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

(defmethod hierarchy/on-activate :pan
  [db]
  (h/set-cursor db "grab"))

(defmethod hierarchy/help [:pan :idle]
  []
  "Click and drag to pan.")

(defmethod hierarchy/on-pointer-up :pan
  [db _e]
  (h/set-cursor db "grab"))

(defmethod hierarchy/on-pointer-down :pan
  [db _e]
  (h/set-cursor db "grabbing"))

(defmethod hierarchy/on-drag :pan
  [db e]
  (frame.h/pan-by db (mat/sub (:pointer-pos db) (:pointer-pos e))))

(defmethod hierarchy/on-drag-end :pan
  [db _e]
  (let [db (-> (h/set-cursor db "grab")
               (snap.h/update-viewport-tree))]
    (h/add-fx db [::app.fx/persist db])))
