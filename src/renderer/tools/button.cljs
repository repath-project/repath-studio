(ns renderer.tools.button
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.views :as attr]
   [renderer.tools.base :as tools]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.units :as units]))

(derive :button ::tools/custom)
(derive :font-color ::attr/color)

(defmethod tools/properties :button
  []
  {:icon "input-text"
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
  [{:keys [adjusted-pointer-pos tool adjusted-pointer-offset fill stroke]}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        [pos-x pos-y] adjusted-pointer-pos
        attrs {:x (min pos-x offset-x)
               :y (min pos-y offset-y)
               :width (abs (- pos-x offset-x))
               :height (abs (- pos-y offset-y))
               :fill fill
               :font-color "#000000"
               :radius (/ (abs (- pos-y offset-y)) 2)
               :label "button"
               :stroke stroke}]
    (rf/dispatch [:document/set-temp-element {:type tool
                                              :attrs attrs}])))

(defmethod tools/render :button
  [{:keys [attrs] :as element}]
  (let [{:keys [x y width height radius font-color]} attrs]
    [:g (merge
         (select-keys attrs [:id :class :opacity])
         {:on-pointer-up   #(pointer/event-handler % element)
          :on-pointer-down #(pointer/event-handler % element)
          :on-pointer-move #(pointer/event-handler % element)})
     [:rect (merge
             {:ry radius
              :rx radius}
             (select-keys attrs [:x
                                 :y
                                 :width
                                 :height
                                 :stroke
                                 :fill
                                 :stroke-width]))]
     [:text (merge
             {:x (+ (units/unit->px x) (/ (units/unit->px width) 2))
              :y (+ (units/unit->px y) (/ (units/unit->px height) 2))
              :fill font-color
              :dominant-baseline "middle"
              :text-anchor "middle"}
             (select-keys attrs [:width
                                 :font-family
                                 :font-size
                                 :font-weight])) (:label attrs)]]))
