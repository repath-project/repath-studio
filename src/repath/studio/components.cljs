(ns repath.studio.components
  (:require
   [reagent.core :as ra]
   [repath.studio.styles :as styles]
   ["react-svg" :refer [ReactSVG]]))

(defn icon
  [{:keys [icon]}]
  [:> ReactSVG {:class-name "icon" :src (str "icons/" icon ".svg")}])

(defn render-tooltip
  [{:keys [label position]}]
  [:div.tooltip {:style {:top "100%"
                         :left "calc( -50% + 6px )"}}
   label])

(defn icon-button
  [{:keys [icon title action class disabled? tooltip]}]
  (let [tooltip-visible? (ra/atom false)]
    (fn [{:keys [icon title action class disabled? tooltip]}]
      [:button {:class    ["icon-button" class (when disabled? " disabled")]
                :title    title
                :on-mouse-enter #(reset! tooltip-visible? true)
                :on-mouse-leave #(reset! tooltip-visible? false)
                :on-click #((.stopPropagation %)
                            (action %))}
       [repath.studio.components/icon {:icon icon}]
       (when (and tooltip @tooltip-visible?) [render-tooltip tooltip])])))

(defn toggle-icon-button
  [{:keys [active? active-icon inactive-icon active-text inactive-text action class]}]
  [:button {:class    ["icon-button" class]
            :title    (if active? active-text inactive-text)
            :on-click #((.stopPropagation %) (action))}
   [icon {:icon        (if active? active-icon inactive-icon)
          :fixed-width true}]])

(defn toggle-collapsed-icon
  [collapsed?]
  [:span {:style {:padding "4px"} :class "collapse-button"} [icon {:icon (if collapsed? "chevron-right" "chevron-down")}]])

(defn toggle-collapsed-button
  [collapsed? action]
  [toggle-icon-button {:active? collapsed?
                       :active-icon "chevron-right"
                       :active-text "expand"
                       :class "collapse-button"
                       :inactive-icon "chevron-down"
                       :inactive-text "collapse"
                       :action action}])


(defn radio-icon-button
  [{:keys [active? icon title action class]}]
  [:button {:title    title
            :class    ["icon-button" class]
            :style    {:color (when active? styles/font-color-active)
                       :background (when active? styles/active)}
            :on-click action}
   [repath.studio.components/icon {:icon icon}]])