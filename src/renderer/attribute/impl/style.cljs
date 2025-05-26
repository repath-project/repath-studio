(ns renderer.attribute.impl.style
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Reference/Attribute/style"
  (:require
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.element.events :as-alias element.events]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/form-element [:default :style]
  [_ k v {:keys [disabled]}]
  [:div.w-full.bg-primary.p-1
   [views/cm-editor v {:on-blur #(rf/dispatch [::element.events/set-attr k %])
                       :attrs {:id (name k)}
                       :options {:mode "css"
                                 :readOnly disabled}}]])
