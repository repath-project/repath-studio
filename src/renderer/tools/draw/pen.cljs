(ns renderer.tools.draw.pen
  (:require
   [clojure.string :as str]
   [renderer.element.handlers :as element.h]
   [renderer.handlers :as handlers]
   [renderer.history.handlers :as history]
   [renderer.tools.base :as tools]
   #_[renderer.tools.path :as path]))

(derive :pen ::tools/draw)

(defmethod tools/properties :pen
  []
  {:icon "pencil"
   :description "Pencil tool"
   :attrs [:stroke-width
           :fill
           :stroke
           :stroke-linejoin
           :opacity]})

(defmethod tools/drag-start :pen
  [db]
  (handlers/set-state db :create))

(defmethod tools/drag :pen
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

(defmethod tools/drag-end :pen
  [db]
  (let [path (-> (element.h/get-temp db)
                 #_(tools/->path)
                 #_(path/manipulate :smooth))]
    (-> db
        (element.h/set-temp path)
        element.h/add
        (history/finalize "Draw line"))))
