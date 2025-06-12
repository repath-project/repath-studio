(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [clojure.string :as string]
   [re-frame.core :as rf]
   [reagent.core :as reagent]
   [renderer.document.handlers :as document.handlers]
   [renderer.element.handlers :as element.handlers]
   [renderer.element.hierarchy :as element.hierarchy]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :brush ::tool.hierarchy/draw)

(defmethod tool.hierarchy/properties :brush
  []
  {:icon "brush"})

(defonce brush-element (reagent/atom nil))

(rf/reg-fx
 ::update-brush
 (fn [value]
   (reset! brush-element value)))

(defmethod tool.hierarchy/on-pointer-move :brush
  [db e]
  (let [[x y] (:adjusted-pointer-pos db)
        pressure (:pressure e)
        pressure (if (zero? pressure) 1 pressure)
        r (* (/ 16 2) pressure)
        stroke (document.handlers/attr db :stroke)]
    (tool.handlers/add-fx db [::update-brush {:type :element
                                              :tag :circle
                                              :attrs {:cx x
                                                      :cy y
                                                      :r r
                                                      :fill stroke}}])))

(defmethod tool.hierarchy/on-drag-start :brush
  [db e]
  (let [point (string/join " " (conj (:adjusted-pointer-pos db) (:pressure e)))
        stroke (document.handlers/attr db :stroke)]
    (-> db
        (tool.handlers/set-state :create)
        (element.handlers/add {:type :element
                               :tag :brush
                               :attrs {:points point
                                       :stroke stroke
                                       :size 16
                                       :thinning 0.5
                                       :smoothing 0.5
                                       :streamline 0.5}}))))

(defmethod tool.hierarchy/on-drag :brush
  [db e]
  (let [point (string/join " " (conj (:adjusted-pointer-pos db) (:pressure e)))
        id (:id (first (element.handlers/selected db)))]
    (element.handlers/update-el db id (fn [el]
                                        (update-in el
                                                   [:attrs :points]
                                                   #(str % " " point))))))

(defmethod tool.hierarchy/on-drag-end :brush
  [db _e]
  (-> db
      (history.handlers/finalize "Draw brush")
      (tool.handlers/activate :transform)))

(defmethod tool.hierarchy/render :brush
  []
  [element.hierarchy/render @brush-element])
