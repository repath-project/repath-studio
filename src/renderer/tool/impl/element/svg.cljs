(ns renderer.tool.impl.element.svg
  "https://www.w3.org/TR/SVG/struct.html#SVGElement"
  (:require
   [renderer.element.handlers :as element.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :svg ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :svg
  []
  {:icon "svg"})

(defmethod tool.hierarchy/help [:svg :create]
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
        height (cond-> height lock-ratio (min width))]
    (-> (tool.handlers/set-state db :create)
        (element.handlers/deselect-all)
        (element.handlers/add {:tag :svg
                               :type :element
                               :attrs {:x (min x offset-x)
                                       :y (min y offset-y)
                                       :width width
                                       :height height}}))))

(defn update-size
  [db lock-ratio]
  (let [[offset-x offset-y] (or (:nearest-neighbor-offset db)
                                (:adjusted-pointer-offset db))
        [x y] (or (:point (:nearest-neighbor db)) (:adjusted-pointer-pos db))
        width (abs (- x offset-x))
        height (abs (- y offset-y))
        width (cond-> width lock-ratio (min height))
        height (cond-> height lock-ratio (min width))
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id #(-> %
                                           (assoc-in [:attrs :x] (str (min x offset-x)))
                                           (assoc-in [:attrs :y] (str (min y offset-y)))
                                           (assoc-in [:attrs :width] (str width))
                                           (assoc-in [:attrs :height] (str height))))))

(defmethod tool.hierarchy/on-pointer-up :svg
  [db e]
  (if (= (:state db) :create)
    (-> db
        (history.handlers/finalize "Create SVG")
        (tool.handlers/activate :transform))
    (create db (:ctrl-key e))))

(defmethod tool.hierarchy/on-pointer-move :svg
  [db e]
  (cond-> db
    (= (:state db) :create)
    (update-size (:ctrl-key e))))

(defmethod tool.hierarchy/on-drag-start :svg
  [db e]
  (create db (:ctrl-key e)))

(defmethod tool.hierarchy/on-drag :svg
  [db e]
  (update-size db (:ctrl-key e)))

(defmethod tool.hierarchy/on-drag-end :svg
  [db _e]
  (-> db
      (history.handlers/finalize "Create SVG")
      (tool.handlers/activate :transform)))
