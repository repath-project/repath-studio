(ns repath.studio.tools.ellipse
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [repath.studio.elements.views :as element-views]
            [repath.studio.attrs.base :as attrs]
            [clojure.string :as str]
            [clojure.core.matrix :as matrix]))

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
    (elements/set-temp db {:type :ellipse :attrs attrs})))

(defmethod tools/translate :ellipse
  [element [x y]] (-> element
                      (attrs/update-attr :cx + x)
                      (attrs/update-attr :cy + y)))

(defmethod tools/scale :ellipse
  [element [x y] handler]
  (let [[x y] (matrix/div [x y] 2)]
    (cond-> element
      (contains? #{:bottom-right
                   :top-right
                   :middle-right} handler) (-> (attrs/update-attr :rx + x)
                                               (attrs/update-attr :cx + x))
      (contains? #{:bottom-left
                   :top-left
                   :middle-left} handler) (-> (attrs/update-attr :rx - x)
                                              (attrs/update-attr :cx + x))
      (contains? #{:bottom-middle
                   :bottom-right
                   :bottom-left} handler) (-> (attrs/update-attr :cy + y)
                                              (attrs/update-attr :ry + y))
      (contains? #{:top-middle
                   :top-left
                   :top-right} handler) (-> (attrs/update-attr :ry - y)
                                            (attrs/update-attr :cy + y)))))

(defmethod tools/bounds :ellipse
  [{{:keys [cx cy rx ry stroke-width stroke]} :attrs}]
    (let [[cx cy rx ry stroke-width-px] (map units/unit->px [cx cy rx ry stroke-width])
          stroke-width-px (if (str/blank? stroke-width) 1 stroke-width-px)
          [rx ry] (matrix/add [rx ry] (/ (if (str/blank? stroke) 0 stroke-width-px) 2))]
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

(defmethod tools/edit :ellipse
  [element [x y] handler]
  (case handler
    :rx (attrs/update-attr element :rx + x)
    :ry (attrs/update-attr element :ry - y)
    element))

(defmethod tools/render-edit :ellipse
  [{:keys [attrs]}]
  (let [{:keys [cx cy rx ry]} attrs
        [cx cy rx ry] (mapv units/unit->px [cx cy rx ry])]
    [:g {:key :edit-handlers}
     (map element-views/square-handler [{:x (+ cx rx) :y cy :key :rx :type :edit-handler}
                                        {:x cx :y (- cy ry) :key :ry :type :edit-handler}])]))
