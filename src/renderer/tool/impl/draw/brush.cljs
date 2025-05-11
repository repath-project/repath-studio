(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [clojure.string :as string]
   [renderer.document.handlers :as document.handlers]
   [renderer.history.handlers :as history.handlers]
   [renderer.tool.handlers :as tool.handlers]
   [renderer.tool.hierarchy :as tool.hierarchy]))

(derive :brush ::tool.hierarchy/draw)

(defmethod tool.hierarchy/properties :brush
  []
  {:icon "brush"})

(defmethod tool.hierarchy/on-pointer-move :brush
  [db e]
  (let [[x y] (:adjusted-pointer-pos db)
        pressure (:pressure e)
        pressure (if (zero? pressure) 1 pressure)
        r (* (/ 16 2) pressure)]
    (tool.handlers/set-temp db {:type :element
                                :tag :circle
                                :attrs {:cx x
                                        :cy y
                                        :r r
                                        :fill (document.handlers/attr db :stroke)}})))

(defmethod tool.hierarchy/on-drag :brush
  [db e]
  (let [active-document (:active-document db)
        point (string/join " " (conj (:adjusted-pointer-pos db) (:pressure e)))
        points-path [:documents active-document :temp-element :attrs :points]]
    (if (get-in db points-path)
      (update-in db points-path #(str % " " point))
      (tool.handlers/set-temp db {:type :element
                                  :tag :brush
                                  :attrs {:points point
                                          :stroke (document.handlers/attr db :stroke)
                                          :size 16
                                          :thinning 0.5
                                          :smoothing 0.5
                                          :streamline 0.5}}))))

(defmethod tool.hierarchy/on-drag-end :brush
  [db _e]
  (-> db
      (tool.handlers/create-temp-element)
      (tool.handlers/activate :transform)
      (history.handlers/finalize "Draw line")))
