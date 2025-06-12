(ns renderer.tool.impl.extension.blob
  "Custom element for https://blobs.dev/"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :blob ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :blob
  []
  {:icon "blob"})

(defn pointer-delta
  [db]
  (matrix/distance (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
                   (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))))

(defn create
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        radius (pointer-delta db)
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> (tool.handlers/set-state db :create)
        (element.handlers/add {:type :element
                               :tag :blob
                               :attrs {:x (- offset-x radius)
                                       :y (- offset-y radius)
                                       :seed (rand-int 1000000)
                                       :extraPoints 8
                                       :randomness 4
                                       :size (* radius 2)
                                       :fill fill
                                       :stroke stroke}}))))

(defn update-size
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        radius (pointer-delta db)
        {:keys [id parent]} (first (element.handlers/selected db))
        [min-x min-y] (element.hierarchy/bbox (element.handlers/entity db parent))]
    (-> db
        (element.handlers/update-el id #(-> %
                                            (assoc-in [:attrs :x] (str (- offset-x radius)))
                                            (assoc-in [:attrs :y] (str (- offset-y radius)))
                                            (assoc-in [:attrs :size] (str (* radius 2)))))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-pointer-up :blob
  [db _e]
  (if (= (:state db) :create)
    (-> db
        (history.handlers/finalize "Create blob")
        (tool.handlers/activate  :transform))
    (create db)))

(defmethod tool.hierarchy/on-pointer-move :blob
  [db _e]
  (cond-> db
    (= (:state db) :create)
    (update-size)))

(defmethod tool.hierarchy/on-drag-start :blob
  [db _e]
  (create db))

(defmethod tool.hierarchy/on-drag :blob
  [db _e]
  (update-size db))

(defmethod tool.hierarchy/on-drag-end :blob
  [db _e]
  (-> db
      (history.handlers/finalize "Create blob")
      (tool.handlers/activate :transform)))
