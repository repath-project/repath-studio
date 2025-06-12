(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.string :as string]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
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
        (element.handlers/deselect-all)
        (element.handlers/add {:type :element
                               :tag :polyline
                               :attrs {:points (str point-1 " " point-2)
                                       :stroke stroke
                                       :fill "transparent"}}))))

(defmethod tool.hierarchy/on-drag :pen
  [db _e]
  (let [point (string/join " " (:adjusted-pointer-pos db))
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id (fn [el]
                                        (update-in el
                                                   [:attrs :points]
                                                   #(str % " " point))))))

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
