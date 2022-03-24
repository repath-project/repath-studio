(ns repath.studio.tools.ellipse
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [clojure.string :as str]))

(derive :ellipse ::tools/shape)

(defmethod tools/properties :ellipse [] {:icon "ellipse"
                                         :description "The <ellipse> element is an SVG basic shape, used to create ellipses based on a center coordinate, and both their x and y radius."
                                         :attrs [:stroke
                                                 :stroke-width
                                                 :opacity
                                                 :fill
                                                 :style]})

(defmethod tools/drag :ellipse
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:cx offset-x
               :cy offset-y
               :fill   (tools/rgba fill)
               :stroke (tools/rgba stroke)
               :rx (Math/abs (- pos-x offset-x))
               :ry (Math/abs (- pos-y offset-y))}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :ellipse :attrs attrs}))))

(defmethod tools/translate :ellipse
  [element [x y]] (-> element
                      (update-in [:attrs :cx] #(units/transform + x val))
                      (update-in [:attrs :cy] #(units/transform + y val))))

(defmethod tools/bounds :ellipse
  [_ {{:keys [cx cy rx ry]} :attrs}]
    (let [[cx cy rx ry] (map units/unit->px [cx cy rx ry])]
      [(- cx rx) (- cy ry) (+ cx rx 2) (+ cy ry)]))

(defmethod tools/area :ellipse
  [{{:keys [rx ry]} :attrs}]
  (let [[rx ry] (map units/unit->px [rx ry])]
    (* Math/PI rx ry)))

(defmethod tools/path :ellipse
  [{{:keys [cx cy rx ry]} :attrs}]
  (str/join " " ["M" (+ cx rx) cy
                 "A" rx ry 0 0 1 cx (+ cy ry)
                 "A" rx ry 0 0 1 (- cx rx) cy
                 "A" rx ry 0 0 1 (+ cx rx) cy]))
