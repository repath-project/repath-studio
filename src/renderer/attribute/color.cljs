(ns renderer.attribute.color
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#color"
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["@re-path/react-color" :refer [ChromePicker]]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.attribute.views :as v]))

(derive :stroke ::color)
(derive :fill ::color)
(derive :color ::color)

(defmethod hierarchy/form-element ::color
  [k v disabled? initial]
  [:<>
   [v/form-input
    {:key k
     :value v
     :disabled? disabled?
     :placeholder initial}]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger {:asChild true}
     [:button.button.color-drip.ml-px.inline-block
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
        :on-change-complete #(rf/dispatch [:element/set-attr k (.-hex %)])
        :on-change #(rf/dispatch [:element/preview-attr k (.-hex %)])}]]]]])
