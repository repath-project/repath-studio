(ns renderer.attribute.color
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#color"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["@repath-project/react-color" :refer [ChromePicker]]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]
   [renderer.element.events :as-alias element.e]))

(derive :stroke ::color)
(derive :fill ::color)
(derive :color ::color)

(defmethod hierarchy/form-element [:default ::color]
  [_ k v {:keys [disabled] :as attrs}]
  [:div.flex.gap-px
   [v/form-input k v attrs]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger
     {:as-child true
      :disabled disabled}
     [:button.button.color-drip.inline-block
      {:style {:flex "0 0 26px"
               :border "5px solid var(--bg-primary)"
               :background v}}]]
    [:> Popover/Portal
     [:> Popover/Content
      {:sideOffset 5
       :className "popover-content"
       :align "end"}
      [:> ChromePicker
       {:color (or v "")
        :on-change-complete #(rf/dispatch [::element.e/set-attr k (.-hex %)])
        :on-change #(rf/dispatch [::element.e/preview-attr k (.-hex %)])}]]]]])
