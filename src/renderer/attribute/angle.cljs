(ns renderer.attribute.angle
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#angle"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.ui :as ui]))

(defmethod hierarchy/form-element [:default ::angle]
  [_ k v attrs]
  [:div.flex.gap-px
   [v/form-input k v attrs]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger {:as-child true}
     [:button.button.bg-primary.text-muted.h-full
      {:style {:width "26px"}}
      [ui/icon "degrees" {:class "small"}]]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :className "popover-content"
       :align "end"}
      [:div.circular-slider]
      [:> Popover/Arrow {:class "popover-arrow"}]]]]])
