(ns repath.studio.tools.circle
  (:require [clojure.string :as str]
            [repath.studio.tools.base :as tools]
            [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.attrs.base :as attrs]
            [clojure.core.matrix :as matrix]
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
        radius (Math/sqrt (apply + (matrix/pow (matrix/sub adjusted-mouse-pos adjusted-mouse-offset) 2)))
        attrs {:cx offset-x
               :cy offset-y
               :fill   (tools/rgba fill)
               :stroke (tools/rgba stroke)
               :r radius}]
    (elements/set-temp db {:type :circle :attrs attrs})))

(defmethod tools/translate :circle
  [element [x y]] (-> element
                      (attrs/update-attr :cx + x)
                      (attrs/update-attr :cy + y)))

(defmethod tools/scale :circle
  [element [x y] handler]
  (update-in element [:attrs :r] #(units/transform + x %)))

(defmethod tools/bounds :circle
  [{{:keys [cx cy r stroke-width stroke]} :attrs}]
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

(defmethod tools/edit :circle
  [element [x _] handler]
  (case handler
    :r (attrs/update-attr element :r + x)
    element))

(defmethod tools/render-edit :circle
  [{{:keys [cx cy r]} :attrs}]
  (let [[cx cy r] (mapv units/unit->px [cx cy r])]
    [element-views/square-handler {:x (+ cx r) :y cy :key :r :type :edit-handler}]))
