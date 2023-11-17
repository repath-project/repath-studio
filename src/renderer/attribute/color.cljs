(ns renderer.attribute.color
  "https://developer.mozilla.org/en-US/docs/Web/SVG/Content_type#color"
  (:require
   [renderer.attribute.views :as views]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as hierarchy]
   ["@re-path/react-color" :refer [ChromePicker]]
   ["@radix-ui/react-popover" :as Popover]))

(derive :stroke ::color)
(derive :fill ::color)
(derive :color ::color)

(defmethod hierarchy/form-element ::color
  [key value disabled? initial]
  [:<>
   [views/form-input {:key key
                      :value value
                      :disabled? disabled?
                      :placeholder initial}]
   [:> Popover/Root {:modal true}
    [:> Popover/Trigger {:asChild true}
     [:button.color-drip.ml-px.inline-block
      {:style {:flex "0 0 26px"
               :border "5px solid var(--level-2)"
               :background value}}]]
    [:> Popover/Portal
     [:> Popover/Content {:sideOffset 5
                          :className "popover-content"
                          :align "end"}
      [:> ChromePicker
       {:color (or value "")
        :on-change-complete #(rf/dispatch [:element/set-attribute key (.-hex %)])
        :on-change #(rf/dispatch [:element/preview-attribute key (.-hex %)])}]]]]])