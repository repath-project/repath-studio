(ns renderer.attribute.impl.angle
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#angle"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [renderer.attribute.hierarchy :as attribute.hierarchy]
   [renderer.attribute.views :as attribute.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.views :as views]))

(defmethod attribute.hierarchy/form-element [:default ::angle]
  [_ k v attrs]
  [:div.flex.gap-px.w-full
   [attribute.views/form-input k v attrs]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger
     {:title (t [::pick-angle "Pick angle"])
      :class "form-control-button"}
     [views/icon "degrees"]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :className "popover-content"
       :align "end"}
      [:div.circular-slider]
      [:> Popover/Arrow {:class "fill-secondary"}]]]]])
