(ns renderer.attribute.style
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.codemirror.views :as cm]))

(defmethod hierarchy/form-element :style
  [key value disabled?]
  [:div.w-full.level-2.py-0.px-2
   [cm/editor value {:options {:readOnly disabled?}
                     :on-blur #(rf/dispatch [:element/set-attribute key %])}]])
