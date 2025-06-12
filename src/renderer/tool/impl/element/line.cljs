(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :line ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :line
  []
  {:icon "line-tool"})

(defn create
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/deselect-all)
        (element.handlers/add {:type :element
                               :tag :line
                               :attrs {:x1 offset-x
                                       :y1 offset-y
                                       :x2 x
                                       :y2 y
                                       :stroke stroke}}))))

(defn update-end
  [db]
  (let [[x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        id (:id (first (element.handlers/selected db)))
        x (.toFixed x 3)
        y (.toFixed y 3)]
    (element.handlers/update-el db id #(-> %
                                           (assoc-in [:attrs :x2] (str x))
                                           (assoc-in [:attrs :y2] (str y))))))

(defmethod tool.hierarchy/on-pointer-move :line
  [db _e]
  (cond-> db
    (= (:state db) :create)
    (update-end)))

(defmethod tool.hierarchy/on-pointer-up :line
  [db _e]
  (if (= (:state db) :create)
    (-> db
        (history.handlers/finalize "Create line")
        (tool.handlers/activate :transform))
    (create db)))

(defmethod tool.hierarchy/on-drag-start :line
  [db _e]
  (create db))

(defmethod tool.hierarchy/on-drag :line
  [db _e]
  (update-end db))

(defmethod tool.hierarchy/on-drag-end :line
  [db _e]
  (-> db
      (history.handlers/finalize "Create line")
      (tool.handlers/activate :transform)))
