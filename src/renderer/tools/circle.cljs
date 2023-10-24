(ns renderer.tools.circle
  "https://www.w3.org/TR/SVG/shapes.html#CircleElement"
  (:require [clojure.string :as str]
            [renderer.tools.base :as tools]
            [renderer.elements.handlers :as elements]
            [renderer.overlay :as overlay]
            [renderer.attribute.hierarchy :as hierarchy]
            [clojure.core.matrix :as matrix]
            [renderer.utils.units :as units]
            [re-frame.core :as rf]))

(derive :circle ::tools/shape)

(defmethod tools/properties :circle
  []
  {:icon "circle"
   :description "The <circle> SVG element is an SVG basic shape, used to draw 
                 circles based on a center point and a radius."
   :attrs [:stroke-width
           :opacity
           :fill
           :stroke
           :stroke-dasharray]})

(defmethod tools/drag :circle
  [{:keys [adjusted-mouse-offset active-document adjusted-mouse-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-mouse-offset
        radius (Math/sqrt (apply + (matrix/pow
                                    (matrix/sub adjusted-mouse-pos
                                                adjusted-mouse-offset)
                                    2)))
        attrs {:cx offset-x
               :cy offset-y
               :fill fill
               :stroke stroke
               :r radius}]
    (elements/set-temp db {:type :element :tag :circle :attrs attrs})))

(defmethod tools/translate :circle
  [element [x y]] (-> element
                      (hierarchy/update-attr :cx + x)
                      (hierarchy/update-attr :cy + y)))

(defmethod tools/scale :circle
  [element [x y] handler]
  (cond-> element
    (contains? #{:middle-right} handler)
    (-> (hierarchy/update-attr :cx + (/ x 2))
        (hierarchy/update-attr :r + (/ x 2)))

    (contains? #{:middle-left} handler)
    (-> (hierarchy/update-attr :cx + (/ x 2))
        (hierarchy/update-attr :r - (/ x 2)))

    (contains? #{:bottom-middle} handler)
    (-> (hierarchy/update-attr :cy + (/ y 2))
        (hierarchy/update-attr :r + (/ y 2)))

    (contains? #{:top-middle} handler)
    (-> (hierarchy/update-attr :cy + (/ y 2))
        (hierarchy/update-attr :r - (/ y 2)))))

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
                   "A" r r 0 0 1 (+ cx r) cy
                   "z"])))

(defmethod tools/edit :circle
  [element [x _y] handler]
  (case handler
    :r (hierarchy/update-attr element :r #(abs (+ % x)))
    element))

(defmethod tools/render-edit :circle
  [{:keys [attrs key]}]
  (let [{:keys [cx cy r]} attrs
        [cx cy r] (mapv units/unit->px [cx cy r])
        active-page @(rf/subscribe [:elements/active-page])
        page-pos (mapv
                  units/unit->px
                  [(-> active-page :attrs :x) (-> active-page :attrs :y)])
        [cx cy] (matrix/add page-pos [cx cy])]
    [:g
     [overlay/line cx cy (+ cx r) cy]
     [overlay/label (str (units/->fixed r)) [(+ cx (/ r 2)) cy]]
     [overlay/times cx cy]
     [overlay/square-handler {:x (+ cx r)
                              :y cy
                              :key :r
                              :type :handler
                              :tag :edit
                              :element key}
      ^{:key (str key "-r")}
      [:title "r"]]]))
