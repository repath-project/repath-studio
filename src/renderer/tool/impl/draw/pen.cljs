(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.string :as str]
   [renderer.app.handlers :as app.h]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.element :as element]
   [renderer.utils.path :as path]))

(derive :pen ::hierarchy/element)

(defmethod hierarchy/properties :pen
  []
  {:icon "pencil"
   :description "Pencil tool"})

(defmethod hierarchy/drag :pen
  [db]
  (let [{:keys [active-document adjusted-pointer-pos]} db
        points-path [:documents active-document :temp-element :attrs :points]]
    (if (get-in db points-path)
      (update-in db points-path #(str % " " (str/join " " adjusted-pointer-pos)))
      (element.h/set-temp db {:type :element
                              :tag :polyline
                              :attrs {:points (str/join " " adjusted-pointer-pos)
                                      :stroke (document.h/attr db :stroke)
                                      :fill "transparent"}}))))

(defmethod hierarchy/drag-end :pen
  [db _e]
  (let [path (-> (element.h/temp db)
                 (element/->path)
                 (path/manipulate :smooth)
                 (path/manipulate :simplify))]
    (-> db
        (element.h/set-temp path)
        (element.h/add)
        (app.h/set-state :idle)
        (app.h/explain "Draw line"))))
