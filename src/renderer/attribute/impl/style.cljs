(ns renderer.attribute.impl.style
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.element.events :as-alias element.e]
   [renderer.ui :as ui]))

(defmethod hierarchy/form-element [:default :style]
  [_ k v {:keys [disabled]}]
  [:div.w-full.bg-primary.p-1
   [ui/cm-editor v {:options {:readOnly disabled}
                    :on-blur #(rf/dispatch [::element.e/set-attr k %])}]])
