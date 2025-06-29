(ns renderer.toolbar.status
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
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
   [renderer.worker.subs :as-alias worker.subs]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [::app.subs/adjusted-pointer-pos])]
    [:div.flex-col.font-mono.leading-tight.hidden
     {:class "xl:flex"
      :style {:min-width "100px"}}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (utils.length/to-fixed x)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (utils.length/to-fixed y)]]]))

(def zoom-options
  [{:label "Set to 50%"
    :id "50"
    :action [::frame.events/set-zoom 0.5]}
   {:label "Set to 100%"
    :id "100"
    :action [::frame.events/set-zoom 1]}
   {:label "Set to 200%"
    :id "200"
    :action [::frame.events/set-zoom 2]}
   {:id :divider-1
    :type :separator}
   {:label "Focus selected"
    :id "center-selected"
    :action [::frame.events/focus-selection :original]}
   {:label "Fit selected"
    :id "fit-selected"
    :action [::frame.events/focus-selection :fit]}
   {:label "Fill selected"
    :id "fill-selected"
    :action [::frame.events/focus-selection :fill]}])

(defn zoom-menu
  []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:title "Select zoom level"
     :class "button flex items-center justify-center overlay px-2
             font-mono rounded-sm hover:overlay-2x"
     :side "top"}
    [:div.flex.items-center
     [views/icon "chevron-up"]]]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content
     {:class "menu-content rounded-sm"
      :side "top"
      :align "end"
      :on-key-down #(.stopPropagation %)
      :on-escape-key-down #(.stopPropagation %)}
     (for [item zoom-options]
       ^{:key (:id item)} [views/dropdown-menu-item item])
     [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]])

(def view-radio-buttons
  [{:title "Timeline"
    :active [::app.subs/panel-visible? :timeline]
    :icon "animation"
    :class "hidden sm:inline-block shrink-0"
    :action [::app.events/toggle-panel :timeline]}
   {:title "Grid"
    :active [::app.subs/grid]
    :icon "grid"
    :class "shrink-0"
    :action [::app.events/toggle-grid]}
   {:title "Rulers"
    :active [::ruler.subs/visible?]
    :icon "ruler-combined"
    :class "shrink-0"
    :action [::ruler.events/toggle-visible]}
   {:title "History"
    :active [::app.subs/panel-visible? :history]
    :icon "history"
    :class "hidden sm:inline-block shrink-0"
    :action [::app.events/toggle-panel :history]}
   {:title "XML"
    :class "hidden sm:inline-block shrink-0"
    :active [::app.subs/panel-visible? :xml]
    :icon "code"
    :action [::app.events/toggle-panel :xml]}])

(defn set-zoom
  [e v]
  (let [new-v (-> (.. e -target -value) js/parseFloat (/ 100))]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (rf/dispatch [::frame.events/set-zoom new-v]))))

(defn zoom-decimal-points
  [zoom]
  (condp > zoom
    1  2
    10 1
    0))

(defn zoom-input
  [zoom]
  (let [value (utils.length/to-fixed (* 100 zoom) (zoom-decimal-points zoom))]
    [:input.form-element.overlay.text-right.font-mono.p-1
     {:key zoom
      :aria-label "Zoom"
      :type "number"
      :input-mode "decimal"
      :min "1"
      :max "10000"
      :style {:width "60px"
              :font-size "inherit"
              :appearance "textfield"}
      :default-value value
      :on-blur #(set-zoom % value)
      :on-key-down #(event.impl.keyboard/input-key-down-handler! % value set-zoom % value)
      :on-wheel #(rf/dispatch (if (pos? (.-deltaY %))
                                [::frame.events/zoom-out]
                                [::frame.events/zoom-in]))}]))

(defn zoom-button-group
  []
  (let [zoom @(rf/subscribe [::document.subs/zoom])]
    [:div.button-group
     [:button.button.overlay.px-2.font-mono.rounded.hover:overlay-2x
      {:disabled (<= zoom 0.01)
       :title "Zoom out"
       :on-click #(rf/dispatch [::frame.events/zoom-out])}
      [views/icon "minus"]]

     [:button.button.overlay.px-2.font-mono.rounded.hover:overlay-2x
      {:disabled (>= zoom 100)
       :title "Zoom in"
       :on-click #(rf/dispatch [::frame.events/zoom-in])}
      [views/icon "plus"]]
     [:div.flex.hidden
      {:class "md:flex"}
      [zoom-input zoom]
      [:div.pr-2.overlay.flex.items-center.font-mono "%"]]
     [zoom-menu]]))

(defn root []
  (let [loading @(rf/subscribe [::worker.subs/loading?])
        fill @(rf/subscribe [::document.subs/fill])
        stroke @(rf/subscribe [::document.subs/stroke])
        get-hex #(:hex (js->clj % :keywordize-keys true))]
    [:div.toolbar.bg-primary.mt-px.relative
     [:div.flex.gap-1
      [views/color-picker
       {:color fill
        :on-change-complete #(rf/dispatch [::element.events/set-attr :fill (get-hex %)])
        :on-change #(rf/dispatch [::document.events/preview-attr :fill (get-hex %)])}

       [:button.button.color-rect
        {:title "Pick fill color"
         :style {:background fill}}]]

      [:button.icon-button
       {:title (t [:color/swap "Swap fill with stroke"])
        :style {:width "21px" :background "transparent"}
        :on-click #(rf/dispatch [::document.events/swap-colors])}
       [views/icon "swap-horizontal"]]
      ;; REVIEW: Can we replace alignOffset with collisionBoundary?
      [views/color-picker
       {:color stroke
        :on-change-complete #(rf/dispatch [::element.events/set-attr :stroke (get-hex %)])
        :on-change #(rf/dispatch [::document.events/preview-attr :stroke (get-hex %)])
        :align-offset -54}
       [:button.button.color-rect.relative
        {:title "Pick stroke color"
         :style {:background stroke}}
        [:div.color-rect.bg-primary.absolute
         {:style {:width "13px"
                  :height "13px"
                  :bottom "9px"
                  :right "9px"}}]]]]
     [:div.grow]
     (when loading
       [:button.icon-button
        [views/loading-indicator]])
     (into [:<>]
           (map (fn [{:keys [title active icon action class]}]
                  [views/radio-icon-button icon @(rf/subscribe active)
                   {:title title
                    :class class
                    :on-click #(rf/dispatch action)}])
                view-radio-buttons))
     [snap.views/root]
     [zoom-button-group]
     [coordinates]
     [timeline.views/time-bar]]))
