(ns renderer.color.views
  (:require
   ["@radix-ui/react-popover" :as Popover]
   ["@repath-project/react-color" :refer [PhotoshopPicker]]
   [re-frame.core :as rf]
   [renderer.color.db :as color.db]
   [renderer.document.events :as-alias document.e]
   [renderer.element.events :as-alias element.e]
   [renderer.ui :as ui]
   [renderer.utils.i18n :refer [t]]))

(defn drip [color]
  [:button.button.color-drip
   {:key (keyword (str color))
    :on-click (fn []
                (rf/dispatch [::document.e/set-fill color])
                (rf/dispatch [::element.e/set-attr :fill color]))
    :style {:background-color color}}
   (when (= color "transparent")
     [:div.bg-primary.text-error.relative
      [ui/icon "times"]])])

(defn swatch [colors]
  [:div.flex (map drip colors)])

#_{:clj-kondo/ignore [:clojure-lsp/unused-public-var]}
(defn palette []
  (into [:div.flex.flex-col.palette] (map swatch color.db/default-palette)))

(defn get-hex
  [color-object]
  (:hex (js->clj color-object :keywordize-keys true)))

(defn swap-button
  []
  [:button.icon-button
   {:title (t [:color/swap "Swap fill with stroke"])
    :style {:width "21px" :background "transparent"}
    :on-click #(rf/dispatch [::document.e/swap-colors])}
   [ui/icon "swap-horizontal"]])

(defn picker
  [color attr props]
  [:> Popover/Root {:modal true}
   [:> Popover/Trigger {:as-child true}
    [:button.button.color-rect.relative {:style {:background color}}
     (when (= attr :stroke)
       [:div.color-rect.bg-primary.absolute
        {:style {:width "13px"
                 :height "13px"
                 :bottom "9px"
                 :right "9px"}}])]]
   [:> Popover/Portal
    [:> Popover/Content
     (merge {:class "popover-content color-picker-lg"
             :align "start"
             :side "top"} props)
     [:> PhotoshopPicker
      {:color color
       :on-change-complete #(rf/dispatch [::element.e/set-attr attr (get-hex %)])
       :on-change #(do (rf/dispatch [::document.e/set-attr attr (get-hex %)])
                       (rf/dispatch [::element.e/preview-attr attr (get-hex %)]))}]
     [:> Popover/Arrow {:class "popover-arrow"}]]]])

