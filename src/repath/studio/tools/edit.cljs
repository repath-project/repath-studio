(ns repath.studio.tools.edit
  (:require [repath.studio.tools.base :as tools]
            [repath.studio.handlers :as handlers]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.history.handlers :as history]
            [clojure.core.matrix :as matrix]))

(derive :edit ::tools/transform)

(defmethod tools/properties :edit [] {:icon "edit"})

(defmethod tools/activate :edit [db] (handlers/set-state db :edit))

(defmethod tools/mouse-down :edit
  [db _ element]
  (assoc db :clicked-element element))

(defmethod tools/drag :edit
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos] :as db}]
  (let [mouse-offset (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)
        db (history/swap db)]
    (elements/update-selected db (fn [elements element] (assoc elements (:key element) (tools/edit element mouse-offset (:key (:clicked-element db))))))))

(defmethod tools/drag-end :edit
  [db _ element]
  (-> db
   (handlers/set-state :default)
   (history/finalize (str "Edit " (-> element :key name)))))