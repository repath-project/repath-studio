(ns renderer.attribute.style
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.codemirror.views :as cm]
   [renderer.element.events :as-alias element.e]))

(defmethod hierarchy/form-element :style
  [k v disabled?]
  [:div.w-full.bg-primary.p-1
   [cm/editor v {:options {:readOnly disabled?}
                 :on-blur #(rf/dispatch [::element.e/set-attr k %])}]])
