(ns renderer.toolbar.status
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["react-resizable-panels" :refer [Panel PanelResizeHandle]]
   [re-frame.core :as rf]
   [renderer.color.views :as color-v]
   [renderer.document.subs :as-alias document.s]
   [renderer.frame.events :as-alias frame.e]
   [renderer.frame.subs :as-alias frame.s]
   [renderer.snap.views :as snap.v]
   [renderer.timeline.views :as timeline.v]
   [renderer.ui :as ui]
   [renderer.utils.keyboard :as keyb]
   [renderer.utils.units :as units]
   [renderer.worker.subs :as-alias worker.s]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [::frame.s/adjusted-pointer-pos])]
    [:div.flex-col.font-mono.leading-tight.hidden.xl:flex
     {:style {:min-width "90px"}}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (units/->fixed x)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (units/->fixed y)]]]))

(def zoom-menu
  [{:label "Set to 50%"
    :key "50"
    :action [::frame.e/set-zoom 0.5]}
   {:label "Set to 100%"
    :key "100"
    :action [::frame.e/set-zoom 1]}
   {:label "Set to 200%"
    :key "200"
    :action [::frame.e/set-zoom 2]}
   {:key :divider-1
    :type :separator}
   {:label "Focus selected"
    :key "center-selected"
    :action [::frame.e/focus-selection :original]}
   {:label "Fit selected"
    :key "fit-selected"
    :action [::frame.e/focus-selection :fit]}
   {:label "Fill selected"
    :key "fill-selected"
    :action [::frame.e/focus-selection :fill]}])

(def view-radio-buttons
  [{:title "Timeline"
    :active? [:panel-visible? :timeline]
    :icon "animation"
    :class "hidden sm:inline-block shrink-0"
    :action [:toggle-panel :timeline]}
   {:title "Grid"
    :active? [:grid-visible?]
    :icon "grid"
    :class "shrink-0"
    :action [:toggle-grid]}
   {:title "Rulers"
    :active? [:rulers-visible?]
    :icon "ruler-combined"
    :class "shrink-0"
    :action [:toggle-rulers]}
   {:title "History"
    :active? [:panel-visible? :history]
    :icon "history"
    :class "hidden sm:inline-block shrink-0"
    :action [:toggle-panel :history]}
   {:title "XML"
    :class "hidden sm:inline-block shrink-0"
    :active? [:panel-visible? :xml]
    :icon "code"
    :action [:toggle-panel :xml]}])

(defn set-zoom
  [e v]
  (let [new-v (-> (.. e -target -value) js/parseFloat (/ 100))]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (rf/dispatch [::frame.e/set-zoom new-v]))))

(defn zoom-decimal-points
  [zoom]
  (condp > zoom
    1 2
    10 1
    0))

(defn zoom-input
  [zoom]
  (let [value (units/->fixed (* 100 zoom) (zoom-decimal-points zoom))]
    [:input.overlay.text-right.hidden.md:flex
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
                                [::frame.e/zoom-out]
                                [::frame.e/zoom-in]))}]))

(defn root []
  (let [zoom @(rf/subscribe [::document.s/zoom])
        timeline? @(rf/subscribe [:panel-visible? :timeline])
        message @(rf/subscribe [:message])
        loading? @(rf/subscribe [::worker.s/loading?])]
    [:<>
     [:div.toolbar.bg-primary.mt-px
      [color-v/picker]
      [:div.grow
       [:div.px-1.hidden.2xl:flex.gap-1.flex-wrap.leading-tight.truncate
        {:style {:max-height "33px"}}
        message]]
      (when loading?
        [:span.icon-button.relative
         [ui/icon "spinner" {:class "loading"}]])
      (into [:<>]
            (map (fn [{:keys [title active? icon action class]}]
                   [ui/radio-icon-button {:title title
                                          :active? @(rf/subscribe active?)
                                          :icon icon
                                          :class class
                                          :action #(rf/dispatch action)}])
                 view-radio-buttons))
      [snap.v/root]
      [:div.button-group
       [:button.button.overlay.px-2.font-mono.rounded
        {:class (when (<= zoom 0.01) "disabled")
         :title "Zoom out"
         :on-click #(rf/dispatch [::frame.e/zoom-out])}
        [ui/icon "minus"]]

       [:button.button.overlay.px-2.font-mono.rounded
        {:class (when (>= zoom 100) "disabled")
         :title "Zoom in"
         :on-click #(rf/dispatch [::frame.e/zoom-in])}
        [ui/icon "plus"]]
       [zoom-input zoom]
       [:div.pr-2.overlay.flex.items-center.hidden.md:flex "%"]

       [:> DropdownMenu/Root
        [:> DropdownMenu/Trigger
         {:class "button flex items-center justify-center overlay px-2 font-mono rounded"
          :side "top"}
         [:div.flex.items-center
          [ui/icon "chevron-up"]]]

        [:> DropdownMenu/Portal
         [:> DropdownMenu/Content
          {:class "menu-content rounded"
           :side "top"
           :align "end"}
          (for [item zoom-menu]
            ^{:key (:key item)} [ui/dropdown-menu-item item])
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
         :defaultSize 20
         :order 2}
        [timeline.v/root]])]))
