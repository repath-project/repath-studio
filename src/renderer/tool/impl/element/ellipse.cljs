(ns renderer.tool.impl.element.ellipse
  "https://www.w3.org/TR/SVG/shapes.html#EllipseElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :ellipse ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :ellipse
  []
  {:icon "ellipse-tool"
   :label "Ellipse"})

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defn attributes
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        rx (.toFixed (abs (- x offset-x)) 3)
        ry (.toFixed (abs (- y offset-y)) 3)]
    {:rx (cond-> rx lock-ratio (min ry))
     :ry (cond-> ry lock-ratio (min rx))}))

(defmethod tool.hierarchy/on-drag-start :ellipse
  [db e]
  (let [[x y] (or (:nearest-neighbor-offset db)
                  (:adjusted-pointer-offset db))
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :ellipse
                               :attrs (merge (attributes db (:ctrl-key e))
                                             {:cx x
                                              :cy y
                                              :fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :ellipse
  [db e]
  (let [attrs (attributes db (:ctrl-key e))
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id #(reduce assoc-attr % attrs))))

(defmethod tool.hierarchy/on-drag-end :ellipse
  [db _e]
  (-> db
      (history.handlers/finalize "Create ellipse")
      (tool.handlers/activate :transform)))
