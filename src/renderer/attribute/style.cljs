(ns renderer.attribute.style
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.codemirror.views :as cm]))

(defmethod hierarchy/form-element :style
  [k v disabled?]
  [:div.w-full.bg-primary.py-0.px-2
   [cm/editor v {:options {:readOnly disabled?}
                 :on-blur #(rf/dispatch [:element/set-attr k %])}]])
