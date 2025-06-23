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
  (matrix/distance (or (:point (:nearest-neighbor db))
                       (:adjusted-pointer-pos db))
                   (or (:nearest-neighbor-offset db)
                       (:adjusted-pointer-offset db))))

(defn attributes
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        radius (pointer-delta db)]
    {:x (.toFixed (- offset-x radius) 3)
     :y (.toFixed (- offset-y radius) 3)
     :size (.toFixed (* radius 2) 3)}))

(defmethod tool.hierarchy/on-drag-start :blob
  [db _e]
  (let [fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)
        seed (rand-int 1000000)]
    (-> (tool.handlers/set-state db :create)
        (element.handlers/add {:type :element
                               :tag :blob
                               :attrs (merge (attributes db)
                                             {:seed seed
                                              :extraPoints 8
                                              :randomness 4
                                              :fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :blob
  [db _e]
  (let [attrs (attributes db)
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        {:keys [id parent]} (first (element.handlers/selected db))
        el (element.handlers/entity db parent)
        [min-x min-y] (element.hierarchy/bbox el)]
    (-> db
        (element.handlers/update-el id #(reduce assoc-attr % attrs))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-drag-end :blob
  [db _e]
  (-> db
      (history.handlers/finalize "Create blob")
      (tool.handlers/activate :transform)))
