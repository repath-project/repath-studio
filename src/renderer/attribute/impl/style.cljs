(ns renderer.attribute.impl.style
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.codemirror.views :as cm]
   [renderer.element.events :as-alias element.e]))

(defmethod hierarchy/form-element [:default :style]
  [_ k v {:keys [disabled]}]
  [:div.w-full.bg-primary.p-1
   [cm/editor v {:options {:readOnly disabled}
                 :on-blur #(rf/dispatch [::element.e/set-attr k %])}]])
