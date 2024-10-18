(ns renderer.tool.impl.element.line
  "https://www.w3.org/TR/SVG/shapes.html#LineElement"
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :line ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :line
  []
  {:icon "line-tool"})

(defn create-line
  [db]
  (let [[offset-x offset-y] (:adjusted-pointer-offset db)
        [x y] (:adjusted-pointer-pos db)
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 x
               :y2 y
               :stroke (document.h/attr db :stroke)}]
    (element.h/set-temp db {:type :element
                            :tag :line
                            :attrs attrs})))

(defn update-line-end
  [db]
  (let [[x y] (:adjusted-pointer-pos db)
        temp (-> (element.h/temp db)
                 (assoc-in [:attrs :x2] x)
                 (assoc-in [:attrs :y2] y))]
    (element.h/set-temp db temp)))

(defmethod tool.hierarchy/pointer-move :line
  [db]
  (cond-> db
    (element.h/temp db) (update-line-end)))

(defmethod tool.hierarchy/pointer-up :line
  [db _e]
  (cond
    (element.h/temp db)
    (-> db
        (element.h/add)
        (app.h/set-tool :transform)
        (app.h/set-state :idle)
        (app.h/explain "Create line"))

    (:pointer-offset db)
    (-> db
        (app.h/set-state :create)
        (create-line))

    :else db))

(defmethod tool.hierarchy/pointer-down :line
  [db _e]
  (cond-> db
    (element.h/temp db)
    (app.h/explain "Create line")))

(defmethod tool.hierarchy/drag :line
  [db]
  (create-line db))
