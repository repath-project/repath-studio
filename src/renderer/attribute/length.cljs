(ns renderer.attribute.length
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#length"
  (:require
   [renderer.attribute.views :as views]
   [renderer.components :as comp]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.utils.units :as units]))

(derive :x ::length)
(derive :y ::length)
(derive :x1 ::length)
(derive :y1 ::length)
(derive :x2 ::length)
(derive :y2 ::length)
(derive :cx ::length)
(derive :cy ::length)
(derive :dx ::length)
(derive :dy ::length)
(derive :width ::length)
(derive :height ::length)
(derive :stroke-width ::length)
(derive :r ::length)
(derive :rx ::length)
(derive :ry ::length)

(defmethod hierarchy/form-element ::length
  [key value disabled? initial]
  [:div.flex.w-full
   [views/form-input {:key key
                      :value value
                      :disabled? disabled?
                      :placeholder (if value initial "multiple")
                      :on-wheel (fn [event]
                                  (if (pos? (.-deltaY event))
                                    (rf/dispatch [:element/dec-attribute key])
                                    (rf/dispatch [:element/inc-attribute key])))}]
   [:div.flex {:style {:width "54px"}}
    [:button.button.ml-px.level-2.text-muted
     {:style {:width "26px" :height "26px"}
      :on-pointer-down #(rf/dispatch [:element/dec-attribute key])}
     [comp/icon "minus" {:class "small"}]]
    [:button.button..ml-px.level-2.text-muted
     {:style {:width "26px" :height "26px"}
      :on-click #(rf/dispatch [:element/inc-attribute key])}
     [comp/icon "plus" {:class "small"}]]]])

(defmethod hierarchy/update-attr ::length
  [element attribute f & args]
  (update-in element [:attrs attribute] #(units/transform f (first args) %)))