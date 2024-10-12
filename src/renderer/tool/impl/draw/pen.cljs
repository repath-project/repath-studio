(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.element :as element]
   [renderer.utils.path :as path]))

(derive :pen ::tool.hierarchy/element)

(defmethod tool.hierarchy/properties :pen
  []
  {:icon "pencil"
   :description "Pencil tool"})

(defmethod tool.hierarchy/drag :pen
  [db]
  (let [{:keys [active-document adjusted-pointer-pos]} db
        stroke (get-in db [:documents active-document :stroke])]
    (if (get-in db [:documents active-document :temp-element :attrs :points])
      (update-in db
                 [:documents active-document :temp-element :attrs :points]
                 #(str % " " (str/join " " adjusted-pointer-pos)))
      (element.h/assoc-temp db {:type :element
                                :tag :polyline
                                :attrs {:points (str/join " " adjusted-pointer-pos)
                                        :stroke stroke
                                        :fill "transparent"}}))))

(defmethod tool.hierarchy/drag-end :pen
  [db _e]
  (let [path (-> (element.h/get-temp db)
                 (element/->path)
                 (path/manipulate :smooth)
                 (path/manipulate :simplify))]
    (-> db
        (element.h/assoc-temp path)
        (element.h/add)
        (app.h/set-state :default)
        (app.h/explain "Draw line"))))
