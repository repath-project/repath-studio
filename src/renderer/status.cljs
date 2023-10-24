(ns renderer.status
  (:require
   [re-frame.core :as rf]
   [renderer.components :as comp]
   [renderer.filters :as filters]
   [re-frame.registrar]
   [renderer.color.views :as color]
   [goog.string :as gstring]
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-select" :as Select]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [:frame/adjusted-mouse-pos])]
    [:div.flex.flex-col.ml-2.font-mono
     {:style {:min-width "80px"}}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (gstring/format "%.2f" x)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (gstring/format "%.2f" y)]]]))
  

(def view-radio-buttons
  [#_{:title "Snap to pixels"
    :active? [:document/snap?]
    :icon "magnet"
    :action [:document/toggle-snap]}
   {:title "Grid"
    :active? [:document/grid?]
    :icon "grid"
    :action [:document/toggle-grid]}
   {:title "Rulers"
    :active? [:document/rulers?]
    :icon "ruler-combined"
    :action [:document/toggle-rulers]}
   #_{:title "History tree"
      :active? @(rf/subscribe [:document/history?])
      :icon "history"
      :action #(rf/dispatch [:document/toggle-history])}
   {:title "XML view"
    :active? [:document/xml?]
    :icon "code"
    :action [:document/toggle-xml]}])

(def zoom-menu
  [{:text "Restore"
    :key "restore-active-page"
    :action [:pan-to-active-page :original]}
   {:text "Zoom to fit"
    :key "fit-active-page"
    :action [:pan-to-active-page :fit]}
   {:text "Zoom to fill"
    :key "fill-active-page"
    :action [:pan-to-active-page :fill]}])

(defn toolbar []
  (let [zoom @(rf/subscribe [:document/zoom])
        _element-colors @(rf/subscribe [:elements/colors])
        filter @(rf/subscribe [:document/filter])]
    [:div.toolbar.footer
     [color/picker]
     [:div.grow [color/palette]]
     #_(when element-colors (map (fn [color] [color-drip (color/hexToRgb color)]) element-colors))
     [:> Select/Root {:value (name filter)
                      :onValueChange #(rf/dispatch [:document/set-filter %])}
      [:> Select/Trigger
       {:class "select-trigger"
        :aria-label "No a11y filter"}
       [:> Select/Value {:placeholder "Filter"}
        [:div.flex.gap-1.justify-between.items-center
         {:style {:min-width "110px"}}
         [:span (name filter)]
         [:> Select/Icon {:class "select-icon"}
          [comp/icon "chevron-up" {:class "small"}]]]]]
      [:> Select/Portal
       [:> Select/Content
        {:side "top"
         :sideOffset 5
         :position "popper"
         :class "menu-content rounded select-content"}
        [:> Select/ScrollUpButton {:class "select-scroll-button"}
         [comp/icon "chevron-up"]]
        [:> Select/Viewport {:class "select-viewport"}
         [:> Select/Group
          [:> Select/Item
           {:value "No a11y filter"
            :class "menu-item select-item"}
           [:> Select/ItemText "No a11y filter"]]
          (map (fn [{:keys [id]}] ^{:key id}
                 [:> Select/Item
                  {:value (name id)
                   :class "menu-item select-item"}
                  [:> Select/ItemText (name id)]]) filters/accessibility)]]
        [:> Select/ScrollDownButton
         {:class "select-scroll-button"}
         [comp/icon "chevron-down"]]]]]
     (into [:<>]
           (map (fn [{:keys [title active? icon action]}]
                  [comp/radio-icon-button {:title title
                                           :active? @(rf/subscribe active?)
                                           :icon icon
                                           :action #(rf/dispatch action)}])
                view-radio-buttons))

     [:div.button-group
      [:button.button.level-3.px-2.font-mono.rounded
       [:div.flex.items-center
        {:class (when (<= zoom 0.01) "disabled")
         :on-click #(rf/dispatch [:zoom-out])}
        [comp/icon "minus" {:class "small"}]]]

      [:button.button.level-3.px-2.font-mono.rounded
       [:div.flex.items-center
        {:class (when (>= zoom 100) "disabled")
         :on-click #(rf/dispatch [:zoom-in])}
        [comp/icon "plus" {:class "small"}]]]

      [:> DropdownMenu/Root
       [:> DropdownMenu/Trigger
        {:class "button flex items-center justify-center level-3 px-2 font-mono rounded"
         :side "top"}
        [:div
         {:style {:min-width "80px"}}
         (str (gstring/format "%.2f" (* 100 zoom)) "%")]
        [:div.flex.items-center
         [comp/icon "chevron-up" {:class "small"}]]]
       [:> DropdownMenu/Portal
        [:> DropdownMenu/Content
         {:class "menu-content rounded"
          :side "top"}
         (map (fn [item] ^{:key (:key item)} [comp/dropdown-menu-item item]) zoom-menu)
         [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]
     [coordinates]]))