(ns repath.studio.color.views
  (:require
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [repath.studio.components :as comp]
   [repath.studio.tools.base :as tools]
   [repath.studio.styles :as styles]
   ["@fluentui/react" :as fui]
   ["react-color" :refer [PhotoshopPicker]]))

(defn drip [color]
  [:div {:key (keyword (str color))
         :on-click #(rf/dispatch [:document/set-fill color])
         :class "color-drip"
         :style {:background-color (tools/rgba color)}}])

(defn swatch [colors]
  [:div.h-box {:style {:flex "1 1 100%"}} (map drip colors)])

(defn palette []
  (let [palette @(rf/subscribe [:color-palette])]
    (into [:div.v-box.palette] (map swatch palette))))

(defn picker
  [fill stroke]
  (let [picker (ra/atom nil)]
    (fn [fill stroke]
      [:div.picker
       [:button {:title    "Swap"
                 :class    "button"
                 :on-click #(rf/dispatch [:document/swap-colors])
                 :style    {:padding  0
                            :position "absolute"
                            :bottom   "-6px"
                            :left     "-6px"}} [comp/icon {:icon "swap"}]]
       [:div {:class "color-rect"
              :style {:background (tools/rgba stroke)
                      :bottom     0
                      :right      0}}
        [:div {:class "color-rect"
               :style {:width      styles/icon-size
                       :height     styles/icon-size
                       :bottom     "7px"
                       :right      "7px"
                       :background styles/level-2}}]]
       [:button {:class "color-rect"
                 :on-click #(if @picker
                              (reset! picker nil)
                              (reset! picker (.-target %)))
                 :style {:background (tools/rgba fill)}}]

       (when @picker [:> fui/Callout {:styles {:root {:padding "0" :z-index "1000"}}
                                      :onDismiss #(reset! picker nil)
                                      :target @picker}
                      [:> PhotoshopPicker
                       {:color (tools/rgba fill)
                        :on-change-complete #(rf/dispatch [:elements/set-attribute :fill (:hex (js->clj % :keywordize-keys true))])
                        :on-change #((rf/dispatch [:document/set-fill (vals (:rgb (js->clj % :keywordize-keys true)))]))}]])])))