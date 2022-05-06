(ns repath.studio.tools.line
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.elements.views :as element-views]
            [repath.studio.tools.base :as tools]
            [repath.studio.attrs.base :as attrs]
            [repath.studio.units :as units]
            [clojure.core.matrix :as matrix]
            [repath.studio.handlers :as handlers]
            [repath.studio.history.handlers :as history]))

(derive :line ::tools/shape)

(defmethod tools/properties :line [] {:icon "line"
                                      :description "The <line> element is an SVG basic shape used to create a line connecting two points."
                                      :attrs [:stroke-width
                                              :opacity]})

(defmethod tools/drag :line
  [{:keys [state adjusted-mouse-offset adjusted-mouse-pos active-document] :as db} event element]
  (if (or (= state :edit) (= (:type element) :edit-handler))
    (let [[x y] (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)
          db (cond-> db
               (= (:type element) :edit-handler) (assoc :edit (:key element))
               :always (handlers/set-state :edit))]
      (elements/update-selected (history/swap db) (fn [elements element]
                                                    (assoc elements (:key element) (case (:edit db)
                                                                                     :starting-point (-> element
                                                                                                         (update-in [:attrs :x1] #(units/transform + x %))
                                                                                                         (update-in [:attrs :y1] #(units/transform + y %)))
                                                                                     :ending-point (-> element
                                                                                                       (update-in [:attrs :x2] #(units/transform + x %))
                                                                                                       (update-in [:attrs :y2] #(units/transform + y %))))))))
    (let [stroke (get-in db [:documents active-document :stroke])
          [offset-x offset-y] adjusted-mouse-offset
          [pos-x pos-y] adjusted-mouse-pos
          attrs {:x1 offset-x
                 :y1 offset-y
                 :x2 pos-x
                 :y2 pos-y
                 :stroke (tools/rgba stroke)}]
      (-> db
          (handlers/set-state :create)
          (elements/set-temp {:type :line :attrs attrs})))))

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

(defmethod tools/edit :line
  [{:keys [attrs] :as element} zoom]
  (let [{:keys [x1 y1 x2 y2]} attrs
        [x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])
        handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)]
    [:g {:key :edit-handlers}
     (map element-views/square-handler [{:x x1 :y y1 :size handler-size :stroke-width stroke-width :key :starting-point :type :edit-handler}
                                        {:x x2 :y y2 :size handler-size :stroke-width stroke-width :key :ending-point :type :edit-handler}])]))

(defmethod tools/path :line
  [{{:keys [x1 y1 x2 y2]} :attrs}]
  (let [[x1 y1 x2 y2] (mapv units/unit->px [x1 y1 x2 y2])]
   (str "M" x1 "," y1 " L" x2 "," y2)))
