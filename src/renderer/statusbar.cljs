(ns renderer.statusbar
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-select" :as Select]
   [goog.string :as gstring]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.color.views :as color-v]
   [renderer.components :as comp]
   [renderer.filters :as filters]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [:frame/adjusted-pointer-pos])]
    [:div.flex.flex-col.ml-2.font-mono
     {:style {:min-width "80px"}}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (gstring/format "%.2f" x)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (gstring/format "%.2f" y)]]]))

(def zoom-menu
  [{:label "Set to 50%"
    :key "50"
    :action [:set-zoom 0.5]}
   {:label "Set to 100%"
    :key "100"
    :action [:set-zoom 1]}
   {:label "Set to 200%"
    :key "200"
    :action [:set-zoom 2]}
   {:key :divider-1
    :type :separator}
   {:label "Initial"
    :key "restore-active-page"
    :action [:pan-to-active-page :original]}
   {:label "Fit active page"
    :key "fit-active-page"
    :action [:pan-to-active-page :fit]}
   {:label "Fill active page"
    :key "fill-active-page"
    :action [:pan-to-active-page :fill]}])

(def view-radio-buttons
  [#_{:title "Snap to pixels"
      :active? [:document/snap?]
      :icon "magnet"
      :action [:document/toggle-snap]}
   {:title "Grid"
    :active? [:grid?]
    :icon "grid"
    :action [:toggle-grid]}
   {:title "Rulers"
    :active? [:rulers?]
    :icon "ruler-combined"
    :action [:toggle-rulers]}
   #_{:title "History tree"
      :active? @(rf/subscribe [:panel/visible? :history])
      :icon "history"
      :action #(rf/dispatch [:panel/toggle :history])}
   {:title "XML view"
    :active? [:panel/visible? :xml]
    :icon "code"
    :action [:panel/toggle :xml]}])

(defn root []
  (let [zoom @(rf/subscribe [:document/zoom])
        _element-colors @(rf/subscribe [:element/colors])
        filter @(rf/subscribe [:document/filter])]
    [:div.toolbar.footer
     [color-v/picker]
     [:div.grow [color-v/palette]]
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
         (map (fn [item]
                ^{:key (:key item)}
                [comp/dropdown-menu-item item])
              zoom-menu)
         [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]
     [coordinates]]))
