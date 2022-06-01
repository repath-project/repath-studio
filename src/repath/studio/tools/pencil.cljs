(ns repath.studio.tools.pencil
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.history.handlers :as history]
            [repath.studio.handlers :as handlers]
            [clojure.string :as str]))

(derive :pencil ::tools/draw)

(defmethod tools/properties :pencil [] {:icon "pencil"
                                        :description "dsfdsf"
                                        :attrs [:stroke-width
                                                :fill
                                                :stroke
                                                :stroke-linejoin
                                                :opacity]})

(defmethod tools/drag-start :pencil
  [db]
  (handlers/set-state db :create))

(defmethod tools/drag :pencil
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])]
    (if (get-in db [:documents active-document :temp-element :attrs :points])
      (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " adjusted-mouse-pos)))
      (elements/set-temp db {:type :element :tag :polyline :attrs {:points (str/join " " adjusted-mouse-pos)
                                                                   :stroke (tools/rgba stroke)
                                                                   :fill "transparent"}}))))

(defmethod tools/drag-end :pencil
  [db]
    (-> db
        (elements/create-from-temp)
        (history/finalize (str "Draw line"))))