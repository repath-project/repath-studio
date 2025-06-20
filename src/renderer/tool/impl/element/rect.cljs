(ns renderer.tool.impl.element.rect
  "https://www.w3.org/TR/SVG/shapes.html#RectElement"
  (:require
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :rect ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :rect
  []
  {:icon "rectangle-tool"
   :label "Rectangle"})

(defmethod tool.hierarchy/help [:rect :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defn attributes
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (.toFixed (abs (- x offset-x)) 3)
        height (.toFixed (abs (- y offset-y)) 3)]
    {:x (.toFixed (min x offset-x) 3)
     :y (.toFixed (min y offset-y) 3)
     :width (cond-> width lock-ratio (min height))
     :height (cond-> height lock-ratio (min width))}))

(defmethod tool.hierarchy/on-drag-start :rect
  [db e]
  (let [fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :rect
                               :attrs (merge (attributes db (:ctrl-key e))
                                             {:fill fill
                                              :stroke stroke})}))))

(defmethod tool.hierarchy/on-drag :rect
  [db e]
  (let [attrs (attributes db (:ctrl-key e))
        assoc-attr (fn [el [k v]] (assoc-in el [:attrs k] (str v)))
        {:keys [id parent]} (first (element.handlers/selected db))
        [min-x min-y] (element.hierarchy/bbox (element.handlers/entity db parent))]
    (-> db
        (element.handlers/update-el id #(reduce assoc-attr % attrs))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-drag-end :rect
  [db _e]
  (-> db
      (history.handlers/finalize "Create rectangle")
      (tool.handlers/activate :transform)))
