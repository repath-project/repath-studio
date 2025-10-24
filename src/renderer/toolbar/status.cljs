(ns renderer.toolbar.status
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   ["@radix-ui/react-tooltip" :as Tooltip]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.events]
   [renderer.app.subs :as-alias app.subs]
   [renderer.document.events :as-alias document.events]
   [renderer.document.subs :as-alias document.subs]
   [renderer.element.events :as-alias element.events]
   [renderer.event.impl.keyboard :as event.impl.keyboard]
   [renderer.frame.events :as-alias frame.events]
   [renderer.ruler.events :as-alias ruler.events]
   [renderer.ruler.subs :as-alias ruler.subs]
   [renderer.snap.views :as snap.views]
   [renderer.timeline.views :as timeline.views]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.length :as utils.length]
   [renderer.views :as views]
   [renderer.window.subs :as-alias window.subs]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [::app.subs/adjusted-pointer-pos])]
    [:div.flex-col.font-mono.leading-tight.hidden
     {:class "xl:flex"
      :style {:min-width "90px"}
      :dir "ltr"}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (utils.length/->fixed x 2 false)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (utils.length/->fixed y 2 false)]]]))

(defn zoom-options []
  [{:label (t [::zoom-set-50 "Set to 50%"])
    :id "50"
    :action [::frame.events/set-zoom 0.5]}
   {:label (t [::zoom-set-100 "Set to 100%"])
    :id "100"
    :action [::frame.events/set-zoom 1]}
   {:label (t [::zoom-set-200 "Set to 200%"])
    :id "200"
    :action [::frame.events/set-zoom 2]}
   {:id :divider-1
    :type :separator}
   {:label (t [::zoom-focus-selected "Focus selected"])
    :id "center-selected"
    :action [::frame.events/focus-selection :original]}
   {:label (t [::zoom-fit-selected "Fit selected"])
    :id "fit-selected"
    :action [::frame.events/focus-selection :fit]}
   {:label (t [::zoom-fill-selected "Fill selected"])
    :id "fill-selected"
    :action [::frame.events/focus-selection :fill]}])

(defn zoom-menu
  []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:title (t [::select-zoom "Select zoom level"])
     :side "top"
     :as-child true}
    [:button.button.flex.items-center.justify-center.px-2.font-mono

     [views/icon "chevron-up"]]]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content
     {:class "menu-content rounded-sm"
      :side "top"
      :align "end"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     (for [item (zoom-options)]
       ^{:key (:id item)}
       [views/dropdown-menu-item item])
     [:> DropdownMenu/Arrow {:class "fill-primary"}]]]])

(defn view-radio-buttons []
  (cond-> []
    @(rf/subscribe [::window.subs/breakpoint? :md])
    (into [{:title (t [::timeline "Timeline"])
            :active [::app.subs/panel-visible? :timeline]
            :icon "animation"
            :action [::app.events/toggle-panel :timeline]}
           {:title (t [::history "History"])
            :active [::app.subs/panel-visible? :history]
            :icon "history"
            :action [::app.events/toggle-panel :history]}
           {:title (t [::xml "XML"])
            :active [::app.subs/panel-visible? :xml]
            :icon "code"
            :action [::app.events/toggle-panel :xml]}])

    :always
    (into [{:title (t [::grid "Grid"])
            :active [::app.subs/grid]
            :icon "grid"
            :action [::app.events/toggle-grid]}
           {:title (t [::rulers "Rulers"])
            :active [::ruler.subs/visible?]
            :icon "ruler-combined"
            :action [::ruler.events/toggle-visible]}])))

(defn set-zoom
  [e v]
  (let [new-v (-> (.. e -target -value) js/parseFloat (/ 100))]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (rf/dispatch [::frame.events/set-zoom new-v]))))

(defn zoom-decimal-points
  [zoom]
  (condp > zoom
    1 2
    10 1
    0))

(defn zoom-input
  [zoom]
  (let [precision (zoom-decimal-points zoom)
        value (utils.length/->fixed (* 100 zoom) precision false)]
    [:input.text-right.font-mono.p-1
     {:key zoom
      :aria-label (t [::zoom "Zoom"])
      :type "number"
      :input-mode "decimal"
      :min "1"
      :max "10000"
      :style {:width "60px"
              :font-size "inherit"
              :appearance "textfield"}
      :default-value value
      :on-blur #(set-zoom % value)
      :on-key-down #(event.impl.keyboard/input-key-down-handler! % value
                                                                 set-zoom
                                                                 % value)
      :on-wheel #(rf/dispatch (if (pos? (.-deltaY %))
                                [::frame.events/zoom-out]
                                [::frame.events/zoom-in]))}]))

(defn zoom-button-group
  []
  (let [zoom @(rf/subscribe [::document.subs/zoom])]
    [:div.button-group.group
     [:button.button.px-2.font-mono.rounded
      {:disabled (<= zoom 0.01)
       :title (t [::zoom-out "Zoom out"])
       :on-click #(rf/dispatch [::frame.events/zoom-out])}
      [views/icon "minus"]]

     [:button.button.px-2.font-mono.rounded
      {:disabled (>= zoom 100)
       :title (t [::zoom-in "Zoom in"])
       :on-click #(rf/dispatch [::frame.events/zoom-in])}
      [views/icon "plus"]]
     [:div.flex.hidden.items-center
      {:class "xl:flex"
       :dir "ltr"}
      [zoom-input zoom]
      [:div.px-2.flex.items-center.font-mono "%"]]
     [zoom-menu]]))

(defn radio-button
  [{:keys [title active icon action class]}]
  [:> Tooltip/Root
   [:> Tooltip/Trigger
    {:as-child true}
    [:span
     [views/radio-icon-button icon @(rf/subscribe active)
      {:class class
       :aria-label title
       :on-click #(rf/dispatch action)}]]]
   [:> Tooltip/Portal
    [:> Tooltip/Content
     {:class "tooltip-content"
      :sideOffset 5
      :side "top"
      :on-escape-key-down #(.stopPropagation %)}
     [:div.flex.gap-2.items-center
      title
      [views/shortcuts action]]]]])

(defn color-selectors []
  (let [fill @(rf/subscribe [::document.subs/fill])
        stroke @(rf/subscribe [::document.subs/stroke])
        get-hex #(:hex (js->clj % :keywordize-keys true))]
    [:div.flex
     [views/color-picker
      {:color fill
       :on-change-complete #(rf/dispatch [::element.events/set-attr :fill
                                          (get-hex %)])
       :on-change #(rf/dispatch [::document.events/preview-attr :fill
                                 (get-hex %)])}

      [:button.button.border.border-border.button-size
       {:title (t [::fill-color "Pick fill color"])
        :style {:background fill}}]]

     [:button.button.bg-transparent!
      {:title (t [::swap-color "Swap fill with stroke"])
       :on-click #(rf/dispatch [::document.events/swap-colors])}
      [views/icon "swap-horizontal"]]

     [views/color-picker
      {:color stroke
       :on-change-complete #(rf/dispatch [::element.events/set-attr
                                          :stroke
                                          (get-hex %)])
       :on-change #(rf/dispatch [::document.events/preview-attr
                                 :stroke
                                 (get-hex %)])}
      [:button.relative.border.border-border.button-size
       {:title (t [::stroke-color "Pick stroke color"])
        :style {:background stroke}}
       [:div.bg-primary.absolute.border.border-border
        {:class "w-1/2 h-1/2 bottom-1/4 right-1/4"}]]]]))

(defn root []
  [views/toolbar
   {:class "bg-primary mt-px relative justify-center md:justify-start py-2
            md:py-1 gap-2 md:gap-1"}
   [color-selectors]
   [:div.grow.hidden.md:block]
   (into [:<>]
         (map radio-button (view-radio-buttons)))
   [snap.views/root]
   [zoom-button-group]
   [coordinates]
   [timeline.views/time-bar]])
