(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.string :as string]
   [renderer.app.effects :as-alias app.effects]
   [renderer.document.handlers :as document.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as utils.element]
   [renderer.utils.path :as utils.path]))

(derive :pen ::tool.hierarchy/draw)

(defmethod tool.hierarchy/properties :pen
  []
  {:icon "pencil"})

(defmethod tool.hierarchy/on-drag :pen
  [db _e]
  (let [{:keys [active-document adjusted-pointer-pos]} db
        points-path [:documents active-document :temp-element :attrs :points]]
    (if (get-in db points-path)
      (update-in db points-path #(str % " " (string/join " " adjusted-pointer-pos)))
      (tool.handlers/set-temp db {:type :element
                                  :tag :polyline
                                  :attrs {:points (string/join " " adjusted-pointer-pos)
                                          :stroke (document.handlers/attr db :stroke)
                                          :fill "transparent"}}))))

(defmethod tool.hierarchy/on-drag-end :pen
  [db _e]
  (let [path (-> (tool.handlers/temp db)
                 (utils.element/->path)
                 (update-in [:attrs :d] utils.path/manipulate :smooth)
                 (update-in [:attrs :d] utils.path/manipulate :simplify))]
    (-> db
        (tool.handlers/set-temp path)
        (tool.handlers/create-temp-element)
        (tool.handlers/activate :transform)
        (history.handlers/finalize "Draw line"))))
