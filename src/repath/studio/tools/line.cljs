(ns repath.studio.tools.line
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [clojure.string :as str]))

(derive :line ::tools/shape)

(defmethod tools/properties :line [] {:icon "line"
                                      :description "The <line> element is an SVG basic shape used to create a line connecting two points."
                                      :attrs [:stroke-width
                                              :opacity]})

(defmethod tools/drag :line
  [{:keys [adjusted-mouse-offset] :as db} _ _ {:keys [adjusted-mouse-pos stroke]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y
               :stroke (tools/rgba stroke)}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :line :attrs attrs}))))

(defmethod tools/move :line
  [element [x y]] (-> element
                      (update-in [:attrs :x1] + x)
                      (update-in [:attrs :x2] + x)
                      (update-in [:attrs :y1] + y)
                      (update-in [:attrs :y2] + y)))

(defmethod tools/bounds :line
  [_ {{:keys [x1 y1 x2 y2]} :attrs}]
  [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)])

(defmethod tools/area :line
  [{{:keys [x1 y1 x2 y2 stroke-width stroke]} :attrs}]
  (let [[x1 y1 x2 y2 stroke-width-px] (map units/unit->px [x1 y1 x2 y2 stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)]
    (* stroke-width-px (Math/hypot (Math/abs (- x1 x2)) (Math/abs (- y1 y2))))))

(defmethod tools/path :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (str "M" x1 "," y1 " L" x2 "," y2))
