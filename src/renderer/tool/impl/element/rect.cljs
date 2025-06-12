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

(defn create
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        width (cond-> width lock-ratio (min height))
        height (cond-> height lock-ratio (min width))
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :rect
                               :attrs {:x (min x offset-x)
                                       :y (min y offset-y)
                                       :width width
                                       :height height
                                       :fill fill
                                       :stroke stroke}}))))

(defn update-size
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (.toFixed (abs (- x offset-x)) 3)
        height (.toFixed (abs (- y offset-y)) 3)
        width (cond-> width lock-ratio (min height))
        height (cond-> height lock-ratio (min width))
        x (.toFixed (min x offset-x) 3)
        y (.toFixed (min y offset-y) 3)
        {:keys [id parent]} (first (element.handlers/selected db))
        [min-x min-y] (element.hierarchy/bbox (element.handlers/entity db parent))]
    (-> db
        (element.handlers/update-el id #(-> %
                                            (assoc-in [:attrs :x] (str x))
                                            (assoc-in [:attrs :y] (str y))
                                            (assoc-in [:attrs :width] (str width))
                                            (assoc-in [:attrs :height] (str height))))
        (element.handlers/translate [(- min-x) (- min-y)]))))

(defmethod tool.hierarchy/on-pointer-up :rect
  [db e]
  (if (= (:state db) :create)
    (-> db
        (history.handlers/finalize "Create rectangle")
        (tool.handlers/activate  :transform))
    (create db (:ctrl-key e))))

(defmethod tool.hierarchy/on-pointer-move :rect
  [db e]
  (cond-> db
    (= (:state db) :create)
    (update-size (:ctrl-key e))))

(defmethod tool.hierarchy/on-drag-start :rect
  [db e]
  (create db (:ctrl-key e)))

(defmethod tool.hierarchy/on-drag :rect
  [db e]
  (update-size db (:ctrl-key e)))

(defmethod tool.hierarchy/on-drag-end :rect
  [db _e]
  (-> db
      (history.handlers/finalize "Create rectangle")
      (tool.handlers/activate :transform)))
