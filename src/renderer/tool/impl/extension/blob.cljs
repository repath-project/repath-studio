(ns renderer.tool.impl.extension.blob
  "Custom element for https://blobs.dev/"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
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

(defmethod tool.hierarchy/on-drag-start :blob
  [db _e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        radius (pointer-delta db)]
    (tool.handlers/set-temp db {:type :element
                                :tag :blob
                                :attrs {:x (- offset-x radius)
                                        :y (- offset-y radius)
                                        :seed (rand-int 1000000)
                                        :extraPoints 8
                                        :randomness 4
                                        :size (* radius 2)
                                        :fill (document.handlers/attr db :fill)
                                        :stroke (document.handlers/attr db :stroke)}})))

(defmethod tool.hierarchy/on-drag :blob
  [db _e]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        radius (pointer-delta db)
        temp (-> (tool.handlers/temp db)
                 (assoc-in [:attrs :x] (- offset-x radius))
                 (assoc-in [:attrs :y] (- offset-y radius))
                 (assoc-in [:attrs :size] (* radius 2)))]
    (tool.handlers/set-temp db temp)))
