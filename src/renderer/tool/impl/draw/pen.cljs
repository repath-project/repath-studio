(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.string :as str]
   [renderer.document.handlers :as document.h]
   [renderer.tool.handlers :as h]
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
      (h/set-temp db {:type :element
                      :tag :polyline
                      :attrs {:points (str/join " " adjusted-pointer-pos)
                              :stroke (document.h/attr db :stroke)
                              :fill "transparent"}}))))

(defmethod hierarchy/drag-end :pen
  [db _e]
  (let [path (-> (h/temp db)
                 (element/->path)
                 (update-in [:attrs :d] path/manipulate :smooth)
                 (update-in [:attrs :d] path/manipulate :simplify))]
    (-> db
        (h/set-temp path)
        (h/create-temp-element)
        (h/set-state :idle)
        (h/explain "Draw line"))))
