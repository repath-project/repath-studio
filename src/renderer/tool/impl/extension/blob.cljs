(ns renderer.tool.impl.extension.blob
  "Custom element for https://blobs.dev/"
  (:require
   [clojure.core.matrix :as mat]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.element.subs :as-alias element.s]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :blob ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :blob
  []
  {:icon "blob"})

(defn pointer-delta
  [db]
  (mat/distance (:adjusted-pointer-pos db) (:adjusted-pointer-offset db)))

(defmethod tool.hierarchy/drag-start :blob
  [db]
  (let [{:keys [stroke fill]} (get-in db [:documents (:active-document db)])
        [offset-x offset-y] (:adjusted-pointer-offset db)
        radius (pointer-delta db)]
    (element.h/assoc-temp db {:type :element
                              :tag :blob
                              :attrs {:x (- offset-x radius)
                                      :y (- offset-y radius)
                                      :seed (rand-int 1000000)
                                      :extraPoints 8
                                      :randomness 4
                                      :size (* radius 2)
                                      :fill fill
                                      :stroke stroke}})))

(defmethod tool.hierarchy/drag :blob
  [db]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        radius (pointer-delta db)
        temp (-> (element.h/get-temp db)
                 (assoc-in [:attrs :x] (- offset-x radius))
                 (assoc-in [:attrs :y] (- offset-y radius))
                 (assoc-in [:attrs :size] (* radius 2)))]
    (element.h/assoc-temp db temp)))
