(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [clojure.string :as str]
   [renderer.app.effects :as-alias app.fx]
   [renderer.document.handlers :as document.h]
   [renderer.history.handlers :as history.h]
   [renderer.tool.handlers :as h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :brush ::hierarchy/draw)

(defmethod hierarchy/properties :brush
  []
  {:icon "brush"})

(defmethod hierarchy/pointer-move :brush
  [db e]
  (let [[x y] (:adjusted-pointer-pos db)
        pressure (:pressure e)
        pressure (if (zero? pressure) 1 pressure)
        r (* (/ 16 2) pressure)]
    (h/set-temp db {:type :element
                    :tag :circle
                    :attrs {:cx x
                            :cy y
                            :r r
                            :fill (document.h/attr db :stroke)}})))

(defmethod hierarchy/drag :brush
  [db e]
  (let [active-document (:active-document db)
        point (str/join " " (conj (:adjusted-pointer-pos db) (:pressure e)))
        points-path [:documents active-document :temp-element :attrs :points]]
    (if (get-in db points-path)
      (update-in db points-path #(str % " " point))
      (h/set-temp db {:type :element
                      :tag :brush
                      :attrs {:points point
                              :stroke (document.h/attr db :stroke)
                              :size 16
                              :thinning 0.5
                              :smoothing 0.5
                              :streamline 0.5}}))))

(defmethod hierarchy/drag-end :brush
  [db _e]
  (-> db
      (h/create-temp-element)
      (h/activate :transform)
      (history.h/finalize "Draw line")))
