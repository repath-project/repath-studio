(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.core.matrix :as matrix]
   [clojure.string :as string]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.path :as utils.path]))

(derive :pen ::tool.hierarchy/draw)

(defmethod tool.hierarchy/properties :pen
  []
  {:icon "pencil"})

(defmethod tool.hierarchy/on-drag-start :pen
  [db _e]
  (let [stroke (document.handlers/attr db :stroke)
        point-1 (string/join " " (:adjusted-pointer-offset db))
        point-2 (string/join " " (:adjusted-pointer-pos db))]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :polyline
                               :attrs {:points (str point-1 " " point-2)
                                       :stroke stroke
                                       :fill "transparent"}}))))

(defmethod tool.hierarchy/on-drag :pen
  [db _e]
  (let [{:keys [id parent]} (first (element.handlers/selected db))
        [min-x min-y] (element.hierarchy/bbox (element.handlers/entity db parent))
        point (matrix/sub (:adjusted-pointer-pos db) [min-x min-y])
        point (string/join " " point)]
    (element.handlers/update-attr db id :points str " " point)))

(defmethod tool.hierarchy/on-drag-end :pen
  [db _e]
  (let [path (-> (first (element.handlers/selected db))
                 (utils.element/->path)
                 (update-in [:attrs :d] utils.path/manipulate :smooth)
                 (update-in [:attrs :d] utils.path/manipulate :simplify))]
    (-> db
        (element.handlers/swap path)
        (history.handlers/finalize "Draw line")
        (tool.handlers/activate :transform))))
