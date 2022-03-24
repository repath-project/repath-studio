(ns repath.studio.tools.circle
  (:require [clojure.string :as str]
            [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.units :as units]))

(derive :circle ::tools/shape)

(defmethod tools/properties :circle [] {:icon "circle"
                                        :description "The <circle> SVG element is an SVG basic shape, used to draw circles based on a center point and a radius."
                                        :attrs [:stroke-width
                                                :opacity
                                                :fill
                                                :stroke]})

(defmethod tools/drag :circle
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        radius (Math/sqrt
                (+ (Math/pow (- pos-x offset-x) 2)
                   (Math/pow (- pos-y offset-y) 2)))
        attrs {:cx offset-x
               :cy offset-y
               :fill   (tools/rgba fill)
               :stroke (tools/rgba stroke)
               :r radius}]
    (-> db
        (assoc :state :create)
        (elements/set-temp {:type :circle :attrs attrs}))))

(defmethod tools/move :circle
  [element [x y]] (-> element
                      (update-in [:attrs :cx] #(units/transform + x %))
                      (update-in [:attrs :cy] #(units/transform + y %))))

(defmethod tools/scale :circle
  [element [x y] handler]
  (update-in element [:attrs :r] #(units/transform + x %)))

(defmethod tools/bounds :circle
  [_ {{:keys [cx cy r stroke-width stroke]} :attrs}]
  (let [[cx cy r stroke-width-px] (map units/unit->px [cx cy r stroke-width])
        stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
        r (+ r (/ (if (str/blank? stroke) 0 stroke-width-px) 2))]
    [(- cx r) (- cy r) (+ cx r) (+ cy r)]))

(defmethod tools/area :circle
  [{{:keys [r]} :attrs}]
    (* Math/PI (Math/pow (units/unit->px r) 2)))

(defmethod tools/path :circle
  [{{:keys [cx cy r]} :attrs}]
  (let [[cx cy r] (map units/unit->px [cx cy r])]
    (str/join " " ["M" (+ cx r) cy
                   "A" r r 0 0 1 cx (+ cy r)
                   "A" r r 0 0 1 (- cx r) cy
                   "A" r r 0 0 1 (+ cx r) cy])))
