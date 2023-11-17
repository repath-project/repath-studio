(ns renderer.color.views
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   ["@radix-ui/react-popover" :as Popover]
   ["@re-path/react-color" :refer [PhotoshopPicker]]))

(defn drip [color]
  [:div.color-drip {:key (keyword (str color))
                    :on-click #(do (rf/dispatch [:document/set-fill color])
                                   (rf/dispatch [:element/set-attribute :fill color]))
                    :style {:background-color color}}
   (when (= color "transparent")
     [:div.level-3.text-error.relative
      [comp/icon "times"]])])

(defn swatch [colors]
  [:div.flex (map drip colors)])

(def color-palette
  [["white" "maroon" "red" "purple" "magenta" "green" "lime"
    "olive" "yellow" "navy" "blue" "teal" "cyan" "transparent"]
   ["black" "#111111" "#222222" "#333333" "#444444" "#555555" "#666666"
    "#777777" "#888888" "#999999" "#aaaaaa" "#cccccc" "#dddddd" "#eeeeee"]])

(defn palette []
  (into [:div.flex.flex-col.palette] (map swatch color-palette)))

(defn get-hex
  [color-object]
  (:hex (js->clj color-object :keywordize-keys true)))

(defn picker
  []
  (let [fill @(rf/subscribe [:document/fill])
        stroke @(rf/subscribe [:document/stroke])]
    [:<>
     [:> Popover/Root {:modal true}
      [:> Popover/Trigger
       [:div.color-rect.relative {:style {:background stroke}}
        [:div.color-rect.level-2.absolute
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
          :on-change-complete #(rf/dispatch [:element/set-attribute :stroke (get-hex %)])
          :on-change #(rf/dispatch [:document/set-stroke (get-hex %)])}]
        [:> Popover/Arrow {:class "popover-arrow"}]]]]

     [:button.icon-button
      {:title "Swap fill with stroke"
       :style {:width "21px"
               :background "transparent"}
       :on-click #(do (.stopPropagation %)
                      (rf/dispatch [:document/swap-colors]))}
      [renderer.components/icon "swap-horizontal"]]

     [:> Popover/Root {:modal true}
      [:> Popover/Trigger
       [:div.color-rect {:style {:background fill}}]]
      [:> Popover/Portal
       [:> Popover/Content
        {:class "popover-content color-picker-lg"
         :align "start"}
        [:> PhotoshopPicker
         {:color fill
          :on-change-complete #(rf/dispatch [:element/set-attribute :fill (get-hex %)])
          :on-change #(rf/dispatch [:document/set-fill (get-hex %)])}]
        [:> Popover/Arrow {:class "popover-arrow"}]]]]]))