(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.handlers :as h]
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
               :stroke (document.h/attr db :stroke)}]
    (h/set-temp db {:type :element
                    :tag :line
                    :attrs attrs})))

(defn update-line-end
  [db]
  (let [[x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        temp (-> (h/temp db)
                 (assoc-in [:attrs :x2] x)
                 (assoc-in [:attrs :y2] y))]
    (h/set-temp db temp)))

(defmethod tool.hierarchy/pointer-move :line
  [db]
  (cond-> db
    (h/temp db)
    (update-line-end)))

(defmethod tool.hierarchy/pointer-up :line
  [db _e]
  (cond
    (h/temp db)
    (-> (h/create-temp-element db)
        (h/activate :transform)
        (history.h/finalize "Create line"))

    (:pointer-offset db)
    (-> (h/set-state db :create)
        (create-line))

    :else db))

(defmethod tool.hierarchy/pointer-down :line
  [db _e]
  (cond-> db
    (h/temp db)
    (history.h/finalize "Create line")))

(defmethod tool.hierarchy/drag :line
  [db]
  (create-line db))
