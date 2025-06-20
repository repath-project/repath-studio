(ns renderer.tool.impl.element.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require
   [clojure.core.matrix :as matrix]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :circle ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :circle
  []
  {:icon "circle-tool"})

(defmethod tool.hierarchy/on-drag-start :circle
  [db _e]
  (let [offset (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        position (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        radius (matrix/distance position offset)
        [cx cy] offset
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> (tool.handlers/set-state db :create)
        (element.handlers/add {:type :element
                               :tag :circle
                               :attrs {:cx cx
                                       :cy cy
                                       :fill fill
                                       :stroke stroke
                                       :r radius}}))))

(defmethod tool.hierarchy/on-drag :circle
  [db _e]
  (let [offset (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        position (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        radius (.toFixed (matrix/distance position offset) 3)
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id #(assoc-in % [:attrs :r] (str radius)))))

(defmethod tool.hierarchy/on-drag-end :circle
  [db _e]
  (-> db
      (history.handlers/finalize "Create circle")
      (tool.handlers/activate :transform)))

(defmethod tool.hierarchy/snapping-points :circle
  [db]
  [(with-meta
     (:adjusted-pointer-pos db)
     {:label (str "Circle " (if (= (:state db) :create) "radius" "center"))})])
