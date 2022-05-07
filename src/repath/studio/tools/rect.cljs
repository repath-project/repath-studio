(ns repath.studio.tools.rect
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [clojure.core.matrix :as matrix]
            [repath.studio.handlers :as handlers]
            [repath.studio.elements.views :as element-views]
            [repath.studio.history.handlers :as history]))

(derive :rect ::tools/shape)

(defmethod tools/properties :rect [] {:icon "rectangle"
                                      :description "The <rect> element is a basic SVG shape that draws rectangles, 
                                                    defined by their position, width, and height. The rectangles 
                                                    may have their corners rounded."
                                      :attrs [:stroke-width
                                              :opacity
                                              :fill
                                              :stroke]})

(defmethod tools/drag :rect
  [{:keys [state adjusted-mouse-offset active-document adjusted-mouse-pos] :as db} event element]
  (if (or (= state :edit) (= (:type element) :edit-handler))
    (let [[offset-x offset-y] (matrix/sub adjusted-mouse-pos adjusted-mouse-offset)
          db (cond-> db
               (= (:type element) :edit-handler) (assoc :edit (:key element))
               :always (handlers/set-state :edit))]
      (elements/update-selected (history/swap db) (fn [elements element]
                                                    (assoc elements (:key element)  (case (:edit db)
                                                                                      :position (-> element
                                                                                                    (update-in [:attrs :x] #(units/transform + offset-x %))
                                                                                                    (update-in [:attrs :y] #(units/transform + offset-y %))
                                                                                                    (update-in [:attrs :width] #(units/transform - offset-x %))
                                                                                                    (update-in [:attrs :height] #(units/transform - offset-y %)))
                                                                                      :size (-> element
                                                                                                (update-in [:attrs :width] #(units/transform + offset-x %))
                                                                                                (update-in [:attrs :height] #(units/transform + offset-y %))))))))
    (let [{:keys [stroke fill]} (get-in db [:documents active-document])
          [offset-x offset-y] adjusted-mouse-offset
          [pos-x pos-y] adjusted-mouse-pos
          attrs {:x      (min pos-x offset-x)
                 :y      (min pos-y offset-y)
                 :width  (Math/abs (- pos-x offset-x))
                 :height (Math/abs (- pos-y offset-y))
                 :fill   (tools/rgba fill)
                 :stroke (tools/rgba stroke)}]
      (elements/set-temp db {:type :rect :attrs attrs}))))

(defmethod tools/area :rect
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))

(defmethod tools/render-edit-handlers :rect
  [{:keys [attrs] :as element} zoom]
  (let [{:keys [x y width height]} attrs
        [x y width height] (mapv units/unit->px [x y width height])
        handler-size (/ 8 zoom)
        stroke-width (/ 1 zoom)]
    [:g {:key :edit-handlers}
     (map element-views/square-handler [{:x x :y y :size handler-size :stroke-width stroke-width :key :position :type :edit-handler}
                                        {:x (+ x width) :y (+ y height) :size handler-size :stroke-width stroke-width :key :size :type :edit-handler}])]))