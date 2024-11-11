(ns renderer.tool.impl.extension.blob
  "Custom element for https://blobs.dev/"
  (:require
   [clojure.core.matrix :as mat]
   [renderer.document.handlers :as document.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :blob ::hierarchy/element)

(defmethod hierarchy/properties :blob
  []
  {:icon "blob"})

(defn pointer-delta
  [db]
  (mat/distance (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
                (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))))

(defmethod hierarchy/on-drag-start :blob
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        radius (pointer-delta db)]
    (h/set-temp db {:type :element
                    :tag :blob
                    :attrs {:x (- offset-x radius)
                            :y (- offset-y radius)
                            :seed (rand-int 1000000)
                            :extraPoints 8
                            :randomness 4
                            :size (* radius 2)
                            :fill (document.h/attr db :fill)
                            :stroke (document.h/attr db :stroke)}})))

(defmethod hierarchy/on-drag :blob
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        radius (pointer-delta db)
        temp (-> (h/temp db)
                 (assoc-in [:attrs :x] (- offset-x radius))
                 (assoc-in [:attrs :y] (- offset-y radius))
                 (assoc-in [:attrs :size] (* radius 2)))]
    (h/set-temp db temp)))
