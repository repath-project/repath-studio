(ns renderer.tools.pen
  (:require [renderer.elements.handlers :as elements]
            [renderer.tools.base :as tools]
            [renderer.history.handlers :as history]
            [renderer.handlers :as handlers]
            #_[renderer.tools.path :as path]
            [clojure.string :as str]))

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
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])]
    (if (get-in db [:documents active-document :temp-element :attrs :points])
      (update-in db
                 [:documents active-document :temp-element :attrs :points]
                 #(str % " " (str/join " " adjusted-mouse-pos)))
      (elements/set-temp db {:type :element
                             :tag :polyline
                             :attrs {:points (str/join " " adjusted-mouse-pos)
                                     :stroke stroke
                                     :fill "transparent"}}))))

(defmethod tools/drag-end :pen
  [db]
  (let [path (-> (elements/get-temp db)
                 #_(tools/->path)
                 #_(path/manipulate :smooth))]
    (-> db
        (elements/set-temp path)
        (elements/create-from-temp)
        (history/finalize (str "Draw line")))))