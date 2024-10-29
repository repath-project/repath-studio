(ns renderer.tool.impl.draw.pen
  (:require
   [clojure.string :as str]
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]
   [renderer.utils.element :as element]
   [renderer.utils.path :as path]))

(derive :pen ::hierarchy/draw)

(defmethod hierarchy/properties :pen
  []
  {:icon "pencil"})

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
        (h/activate :transform)
        (history.h/finalize "Draw line"))))
