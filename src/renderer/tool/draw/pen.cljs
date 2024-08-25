(ns renderer.tool.draw.pen
  (:require
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.history.handlers :as history]
   [renderer.tool.base :as tool]
   [renderer.tool.shape.path :as path]
   [renderer.utils.element :as element]))

(derive :pen ::tool/renderable)

(defmethod tool/properties :pen
  []
  {:icon "pencil"
   :description "Pencil tool"
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :opacity]})

(defmethod tool/drag :pen
  [{:keys [active-document adjusted-pointer-pos] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])]
    (if (get-in db [:documents active-document :temp-element :attrs :points])
      (update-in db
                 [:documents active-document :temp-element :attrs :points]
                 #(str % " " (str/join " " adjusted-pointer-pos)))
      (element.h/set-temp db {:type :element
                              :tag :polyline
                              :attrs {:points (str/join " " adjusted-pointer-pos)
                                      :stroke stroke
                                      :fill "transparent"}}))))

(defmethod tool/drag-end :pen
  [db]
  (let [path (-> (element.h/get-temp db)
                 (element/->path)
                 (path/manipulate :smooth)
                 (path/manipulate :simplify))]
    (-> db
        (element.h/set-temp path)
        element.h/add
        (app.h/set-state :default)
        (history/finalize "Draw line"))))
