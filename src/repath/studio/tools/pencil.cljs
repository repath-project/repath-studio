(ns repath.studio.tools.pencil
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.history.handlers :as history]
            [clojure.string :as str]))

(derive :pencil ::tools/draw)

(defmethod tools/properties :pencil [] {:icon "pencil"
                                        :description "dsfdsf"
                                        :attrs [:stroke-width
                                                :fill
                                                :stroke
                                                :stroke-linejoin
                                                :opacity]})

(defmethod tools/drag :pencil
  [{:keys [active-document adjusted-mouse-pos] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])]
   (if (get-in db [:documents active-document :temp-element :attrs :points])
    (update-in db [:documents active-document :temp-element :attrs :points] #(str % " " (str/join " " adjusted-mouse-pos)))
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :polyline :attrs {:points (str/join " " adjusted-mouse-pos)
                                                    :stroke (tools/rgba stroke)
                                                    :fill "transparent"}})))))

(defmethod tools/drag-end :pencil
  [db _ _]
  (let [temp-element (get-in db [:documents (:active-document db) :temp-element])]
    (if temp-element
      (-> db
          (elements/create temp-element)
          (elements/clear-temp)
          (history/finalize (str "Draw " (name (:type temp-element)))))
      db)))