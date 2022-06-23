(ns repath.studio.tools.button
  (:require [re-frame.core :as rf]
            [repath.studio.tools.base :as tools]
            [repath.studio.attrs.views :as attrs]
            [repath.studio.mouse :as mouse]
            [repath.studio.units :as units]))

(derive :button ::tools/custom)
(derive :font-color ::attrs/color)

(defmethod tools/properties :button [] {:icon "input-text"
                                        :attrs [:text
                                                :x
                                                :y
                                                :width
                                                :height
                                                :radius
                                                :fill
                                                :stroke
                                                :stroke-width
                                                :font-color
                                                :font-family
                                                :font-size
                                                :font-weight
                                                :opacity
                                                :id
                                                :class]})

(defmethod tools/drag :button
  [{:keys [adjusted-mouse-pos tool adjusted-mouse-offset fill stroke]}]
  (let [[offset-x offset-y] adjusted-mouse-offset
        [pos-x pos-y] adjusted-mouse-pos
        attrs {:x      (min pos-x offset-x)
               :y      (min pos-y offset-y)
               :width  (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))
               :fill   (tools/rgba fill)
               :font-color "#000000"
               :radius (/ (abs (- pos-y offset-y)) 2)
               :label   "button"
               :stroke (tools/rgba stroke)}]
    (rf/dispatch [:document/set-temp-element {:type tool
                                     :attrs attrs}])))

(defmethod tools/render :button
  [{:keys [attrs key] :as element}]
  (let [{:keys [x y width height radius font-color]} attrs]
    [:g (merge
         (select-keys attrs [:id :class :opacity])
         {:on-mouse-up   #(mouse/event-handler % element)
          :on-mouse-down #(mouse/event-handler % element)
          :on-mouse-move #(mouse/event-handler % element)})
     [:rect (merge
             {:ry radius
              :rx radius}
             (select-keys attrs [:x :y :width :height :stroke :fill :stroke-width]))]
     [:text (merge
             {:x (+ (units/unit->px x) (/ (units/unit->px width) 2))
              :y (+ (units/unit->px y) (/ (units/unit->px height) 2))
              :fill font-color
              :dominant-baseline "middle"
              :text-anchor "middle"}
             (select-keys attrs [:width :font-family :font-size :font-weight])) (:label attrs)]]))
