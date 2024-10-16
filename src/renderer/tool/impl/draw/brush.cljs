(ns renderer.tool.impl.draw.brush
  "https://github.com/steveruizok/perfect-freehand"
  (:require
   [renderer.app.handlers :as app.h]
   [renderer.document.handlers :as document.h]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as hierarchy]))

(derive :brush ::hierarchy/element)

(defmethod hierarchy/properties :brush
  []
  {:icon "brush"})

(defmethod hierarchy/help [:brush :default]
  []
  "Click and drag to draw.")

(defmethod hierarchy/activate :brush
  [db]
  (app.h/set-cursor db "none"))

(defmethod hierarchy/pointer-move :brush
  [db e]
  (let [[x y] (:adjusted-pointer-pos db)
        pressure (:pressure e)
        pressure (if (zero? pressure) 1 pressure)
        r (* (/ 16 2) pressure)]
    (element.h/assoc-temp db {:type :element
                              :tag :circle
                              :attrs {:cx x
                                      :cy y
                                      :r r
                                      :fill (document.h/attr db :stroke)}})))

(defmethod hierarchy/drag :brush
  [db e]
  (let [active-document (:active-document db)
        stroke (get-in db [:documents active-document :stroke])
        point (conj (:adjusted-pointer-pos db) (:pressure e))
        points-path [:documents active-document :temp-element :attrs :points]]
    (if (get-in db points-path)
      (update-in db points-path conj point)
      (element.h/assoc-temp db {:type :element
                                :tag :brush
                                :attrs {:points [point]
                                        :stroke stroke
                                        :size 16
                                        :thinning 0.5
                                        :smoothing 0.5
                                        :streamline 0.5}}))))

(defmethod hierarchy/drag-end :brush
  [db _e]
  (-> db
      (element.h/add)
      (app.h/set-state :default)
      (app.h/explain "Draw line")))
