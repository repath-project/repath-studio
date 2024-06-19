(ns renderer.color.views
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["@repath-project/react-color" :refer [PhotoshopPicker]]
   [i18n :refer [t]]
   [re-frame.core :as rf]
   [renderer.color.db :as color.db]
   [renderer.components :as comp]))

(defn drip [color]
  [:button.button.color-drip
   {:key (keyword (str color))
    :on-click (fn []
                (rf/dispatch [:document/set-fill color])
                (rf/dispatch [:element/set-attr :fill color]))
    :style {:background-color color}}
   (when (= color "transparent")
     [:div.bg-primary.text-error.relative
      [comp/icon "times"]])])

(defn swatch [colors]
  [:div.flex (map drip colors)])

(defn palette []
  (into [:div.flex.flex-col.palette] (map swatch color.db/default-palette)))

(defn get-hex
  [color-object]
  (:hex (js->clj color-object :keywordize-keys true)))

(defn picker
  []
  (let [fill @(rf/subscribe [:document/fill])
        stroke @(rf/subscribe [:document/stroke])]
    [:<>
     [:> Popover/Root {:modal true}
      [:> Popover/Trigger {:as-child true}
       [:button.button.color-rect.relative {:style {:background stroke}}
        [:div.color-rect.bg-primary.absolute
         {:style {:width "13px"
                  :height "13px"
                  :bottom "9px"
                  :right "9px"}}]]]
      [:> Popover/Portal
       [:> Popover/Content
        {:class "popover-content color-picker-lg"
         :align "start"}
        [:> PhotoshopPicker
         {:color stroke
          :header nil
          :on-change-complete #(rf/dispatch [:element/set-attr :stroke (get-hex %)])
          :on-change #(rf/dispatch [:document/set-stroke (get-hex %)])}]
        [:> Popover/Arrow {:class "popover-arrow"}]]]]

     [:button.icon-button
      {:title (t [:color/swap "Swap fill with stroke"])
       :style {:width "21px"
               :background "transparent"}
       :on-click (fn [e]
                   (.stopPropagation e)
                   (rf/dispatch [:document/swap-colors]))}
      [renderer.components/icon "swap-horizontal"]]

     [:> Popover/Root {:modal true}
      [:> Popover/Trigger {:as-child true}
       [:button.button.color-rect {:style {:background fill}}]]
      [:> Popover/Portal
       [:> Popover/Content
        {:class "popover-content color-picker-lg"
         :align "start"}
        [:> PhotoshopPicker
         {:color fill
          :on-change-complete #(rf/dispatch [:element/set-attr :fill (get-hex %)])
          :on-change #(rf/dispatch [:document/set-fill (get-hex %)])}]
        [:> Popover/Arrow {:class "popover-arrow"}]]]]]))
