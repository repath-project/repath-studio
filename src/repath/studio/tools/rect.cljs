(ns repath.studio.tools.rect
  (:require [repath.studio.elements.handlers :as elements]
            [repath.studio.tools.base :as tools]
            [repath.studio.units :as units]
            [clojure.core.matrix :as matrix]))

(derive :rect ::tools/shape)

(defmethod tools/properties :rect [] {:icon "rectangle"
                                      :description "The <rect> element is a basic SVG shape that draws rectangles, defined by their position, width, and height. The rectangles may have their corners rounded."
                                      :attrs [:stroke-width
                                              :opacity
                                              :fill
                                              :stroke]})

(defmethod tools/drag :rect
  [{:keys [state adjusted-mouse-offset active-document adjusted-mouse-pos adjusted-mouse-diff] :as db} event element]
  (if (or (= state :edit) (= (:type element) :edit-handler))
    (let [[offset-x offset-y] adjusted-mouse-diff
          db (cond-> db
               (= (:type element) :edit-handler) (assoc :edit (:key element))
               :always (assoc :state :edit))]
      (case (:edit db)
        :position (elements/update-selected db (fn [elements element]
                                                 (assoc elements (:key element) (-> element
                                                                                    (update-in [:attrs :x] #(units/transform + offset-x %))
                                                                                    (update-in [:attrs :y] #(units/transform + offset-y %))
                                                                                    (update-in [:attrs :width] #(units/transform - offset-x %))
                                                                                    (update-in [:attrs :height] #(units/transform - offset-y %))))))
        :size (elements/update-selected db (fn [elements element]
                                                   (assoc elements (:key element) (-> element
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
      (-> db
          (assoc :state :create)
          (elements/set-temp {:type :rect :attrs attrs})))))

(defmethod tools/area :rect
  [{{:keys [width height]} :attrs}]
  (apply * (map units/unit->px [width height])))