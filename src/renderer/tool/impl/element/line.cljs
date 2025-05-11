(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :line ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :line
  []
  {:icon "line-tool"})

(defn create-line
  [db]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db) (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 x
               :y2 y
               :stroke (document.handlers/attr db :stroke)}]
    (tool.handlers/set-temp db {:type :element
                                :tag :line
                                :attrs attrs})))

(defn update-line-end
  [db]
  (let [[x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        temp (-> (tool.handlers/temp db)
                 (assoc-in [:attrs :x2] x)
                 (assoc-in [:attrs :y2] y))]
    (tool.handlers/set-temp db temp)))

(defmethod tool.hierarchy/on-pointer-move :line
  [db _e]
  (cond-> db
    (tool.handlers/temp db)
    (update-line-end)))

(defmethod tool.hierarchy/on-pointer-up :line
  [db _e]
  (cond
    (tool.handlers/temp db)
    (-> (tool.handlers/create-temp-element db)
        (tool.handlers/activate :transform)
        (history.handlers/finalize "Create line"))

    (:pointer-offset db)
    (-> (tool.handlers/set-state db :create)
        (create-line))

    :else db))

(defmethod tool.hierarchy/on-pointer-down :line
  [db _e]
  (cond-> db
    (tool.handlers/temp db)
    (history.handlers/finalize "Create line")))

(defmethod tool.hierarchy/on-drag :line
  [db _e]
  (create-line db))
