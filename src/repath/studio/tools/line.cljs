(ns repath.studio.tools.line
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.tools.base :as tools]
            [repath.studio.attrs.base :as attrs]
            [repath.studio.units :as units]))

(derive :line ::tools/shape)

(defmethod tools/properties :line [] {:icon "line"
                                      :description "The <line> element is an SVG basic shape used to create a line connecting two points."
                                      :attrs [:stroke-width
                                              :opacity]})

(defmethod tools/drag :line
  [{:keys [adjusted-mouse-offset adjusted-mouse-pos active-document] :as db}]
  (let [stroke (get-in db [:documents active-document :stroke])
        [offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x1 offset-x
               :y1 offset-y
               :x2 pos-x
               :y2 pos-y
               :stroke (tools/rgba stroke)}]
    (elements/set-temp db {:type :line :attrs attrs})))

(defmethod tools/translate :line
  [element [x y]] (-> element
                      (attrs/update-attr :x1 + x)
                      (attrs/update-attr :y1 + y)
                      (attrs/update-attr :x2 + x)
                      (attrs/update-attr :y2 + y)))

(defmethod tools/bounds :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (let [[x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    [(min x1 x2) (min y1 y2) (max x1 x2) (max y1 y2)]))

(defmethod tools/path :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (let [[x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
   (str "M" x1 "," y1 " L" x2 "," y2)))

(defmethod tools/render-edit :line
  [{:keys [attrs]}]
  (let [{:keys [x1 y1 x2 y2]} attrs
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
    [:g {:key :edit-handlers}
     (map (fn [handler] [element-views/square-handler handler]) [{:x x1 :y y1 :key :starting-point :type :edit-handler}
                                                                 {:x x2 :y y2 :key :ending-point :type :edit-handler}])]))

(defmethod tools/edit :line
  [element [x y] handler]
  (case handler
    :starting-point (-> element
                        (attrs/update-attr :x1 + x)
                        (attrs/update-attr :y1 + y))
    :ending-point (-> element
                      (attrs/update-attr :x2 + x)
                      (attrs/update-attr :y2 + y))
    element))
