(ns renderer.toolbar.status
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["react-resizable-panels" :refer [Panel PanelResizeHandle]]
   [re-frame.core :as rf]
   [re-frame.registrar]
   [renderer.color.views :as color-v]
   [renderer.components :as comp]
   [renderer.snap.views :as snap.v]
   [renderer.timeline.views :as timeline.v]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.units :as units]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [:frame/adjusted-pointer-pos])]
    [:div.flex.flex-col.font-mono.leading-tight
     {:style {:min-width "90px"}}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (units/->fixed x)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (units/->fixed y)]]]))

(def zoom-menu
  [{:label "Set to 50%"
    :key "50"
    :action [:frame/set-zoom 0.5]}
   {:label "Set to 100%"
    :key "100"
    :action [:frame/set-zoom 1]}
   {:label "Set to 200%"
    :key "200"
    :action [:frame/set-zoom 2]}
   {:key :divider-1
    :type :separator}
   {:label "Focus selected"
    :key "center-selected"
    :action [:frame/focus-selection :original]}
   {:label "Fit selected"
    :key "fit-selected"
    :action [:frame/focus-selection :fit]}
   {:label "Fill selected"
    :key "fill-selected"
    :action [:frame/focus-selection :fill]}])

(def view-radio-buttons
  [{:title "Timeline"
    :active? [:panel/visible? :timeline]
    :icon "timeline"
    :action [:panel/toggle :timeline]}
   {:title "Grid"
    :active? [:grid?]
    :icon "grid"
    :action [:toggle-grid]}
   {:title "Rulers"
    :active? [:rulers?]
    :icon "ruler-combined"
    :action [:toggle-rulers]}
   {:title "History"
    :active? [:panel/visible? :history]
    :icon "history"
    :action [:panel/toggle :history]}
   {:title "XML"
    :active? [:panel/visible? :xml]
    :icon "code"
    :action [:panel/toggle :xml]}])

(defn set-zoom
  [e v]
  (let [new-v (-> (.. e -target -value) (js/parseFloat) (/ 100))]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (rf/dispatch [:frame/set-zoom new-v]))))

(defn zoom-decimal-points
  [zoom]
  (condp > zoom
    1 2
    10 1
    0))

(defn zoom-input
  [zoom]
  (let [value (units/->fixed (* 100 zoom) (zoom-decimal-points zoom))]
    [:input.overlay.text-right.flex
     {:key zoom
      :aria-label "Zoom"
      :type "number"
      :input-mode "decimal"
      :min "1"
      :max "10000"
      :style {:width "60px"
              :appearance "textfield"}
      :default-value value
      :on-blur #(set-zoom % value)
      :on-key-down #(keyb/input-key-down-handler % value set-zoom % value)
      :on-wheel #(rf/dispatch (if (pos? (.-deltaY %))
                                [:frame/zoom-out]
                                [:frame/zoom-in]))}]))

(defn root []
  (let [zoom @(rf/subscribe [:document/zoom])
        timeline? @(rf/subscribe [:panel/visible? :timeline])]
    [:<>
     [:div.toolbar.bg-primary
      [color-v/picker]
      [:div.grow.text-xs.truncate.mx-1
       @(rf/subscribe [:message])]
      (into [:<>]
            (map (fn [{:keys [title active? icon action]}]
                   [comp/radio-icon-button {:title title
                                            :active? @(rf/subscribe active?)
                                            :icon icon
                                            :action #(rf/dispatch action)}])
                 view-radio-buttons))
      [snap.v/root]
      [:div.button-group
       [:button.button.overlay.px-2.font-mono.rounded
        {:class (when (<= zoom 0.01) "disabled")
         :title "Zoom out"
         :on-click #(rf/dispatch [:frame/zoom-out])}
        [comp/icon "minus" {:class "icon small"}]]

       [:button.button.overlay.px-2.font-mono.rounded
        {:class (when (>= zoom 100) "disabled")
         :title "Zoom in"
         :on-click #(rf/dispatch [:frame/zoom-in])}
        [comp/icon "plus" {:class "icon small"}]]
       [zoom-input zoom]
       [:div.pr-2.overlay.flex.items-center "%"]

       [:> DropdownMenu/Root
        [:> DropdownMenu/Trigger
         {:class "button flex items-center justify-center overlay px-2 font-mono rounded"
          :side "top"}
         [:div.flex.items-center
          [comp/icon "chevron-up" {:class "icon small"}]]]

        [:> DropdownMenu/Portal
         [:> DropdownMenu/Content
          {:class "menu-content rounded"
           :side "top"
           :align "end"}
          (for [item zoom-menu]
            ^{:key (:key item)} [comp/dropdown-menu-item item])
          [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]]]
      [coordinates]]
     [timeline.v/time-bar]
     (when timeline?
       [:> PanelResizeHandle
        {:id "timeline-resize-handle"
         :className "resize-handle"}])
     (when timeline?
       [:> Panel
        {:id "timeline-panel"
         :minSize 10
         :defaultSize 10
         :order 2}
        [timeline.v/root]])]))
