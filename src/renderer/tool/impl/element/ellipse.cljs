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
  {:icon "ellipse-tool"})

(defmethod tool.hierarchy/help [:ellipse :create]
  []
  [:div "Hold " [:span.shortcut-key "Ctrl"] " to lock proportions."])

(defn create
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))
        rx (cond-> rx lock-ratio (min ry))
        ry (cond-> ry lock-ratio (min rx))
        fill (document.handlers/attr db :fill)
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/deselect-all)
        (element.handlers/add {:type :element
                               :tag :ellipse
                               :attrs {:cx offset-x
                                       :cy offset-y
                                       :fill fill
                                       :stroke stroke
                                       :rx rx
                                       :ry ry}}))))

(defn update-radius
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        rx (abs (- x offset-x))
        ry (abs (- y offset-y))
        rx (cond-> rx lock-ratio (min ry))
        ry (cond-> ry lock-ratio (min rx))
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id #(-> %
                                           (assoc-in [:attrs :rx] (str rx))
                                           (assoc-in [:attrs :ry] (str ry))))))

(defmethod tool.hierarchy/on-pointer-up :ellipse
  [db e]
  (if (= (:state db) :create)
    (-> db
        (history.handlers/finalize "Create ellipse")
        (tool.handlers/activate :transform))
    (create db (:ctrl-key e))))

(defmethod tool.hierarchy/on-pointer-move :ellipse
  [db e]
  (cond-> db
    (= (:state db) :create)
    (update-radius (:ctrl-key e))))

(defmethod tool.hierarchy/on-drag-start :ellipse
  [db e]
  (create db (:ctrl-key e)))

(defmethod tool.hierarchy/on-drag :ellipse
  [db e]
  (update-radius db (:ctrl-key e)))

(defmethod tool.hierarchy/on-drag-end :ellipse
  [db _e]
  (-> db
      (history.handlers/finalize "Create ellipse")
      (tool.handlers/activate :transform)))
