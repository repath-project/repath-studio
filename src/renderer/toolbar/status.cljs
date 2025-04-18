(ns renderer.toolbar.status
  (:require
   ["@radix-ui/react-dropdown-menu" :as DropdownMenu]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.subs :as-alias app.s]
   [renderer.document.events :as-alias document.e]
   [renderer.document.subs :as-alias document.s]
   [renderer.element.events :as-alias element.e]
   [renderer.frame.events :as-alias frame.e]
   [renderer.ruler.events :as-alias ruler.e]
   [renderer.ruler.subs :as-alias ruler.s]
   [renderer.snap.views :as snap.v]
   [renderer.timeline.views :as timeline.v]
   [renderer.tool.subs :as-alias tool.s]
   [renderer.ui :as ui]
   [renderer.utils.i18n :refer [t]]
   [renderer.utils.keyboard :as keyb]
   [renderer.worker.subs :as-alias worker.s]))

(defn coordinates []
  (let [[x y] @(rf/subscribe [::app.s/adjusted-pointer-pos])]
    [:div.flex-col.font-mono.leading-tight.hidden
     {:class "xl:flex"
      :style {:min-width "90px"}}
     [:div.flex.justify-between
      [:span.mr-1 "X:"] [:span (.toFixed x 2)]]
     [:div.flex.justify-between
      [:span.mr-1 "Y:"] [:span (.toFixed y 2)]]]))

(def zoom-options
  [{:label "Set to 50%"
    :id "50"
    :action [::frame.e/set-zoom 0.5]}
   {:label "Set to 100%"
    :id "100"
    :action [::frame.e/set-zoom 1]}
   {:label "Set to 200%"
    :id "200"
    :action [::frame.e/set-zoom 2]}
   {:id :divider-1
    :type :separator}
   {:label "Focus selected"
    :id "center-selected"
    :action [::frame.e/focus-selection :original]}
   {:label "Fit selected"
    :id "fit-selected"
    :action [::frame.e/focus-selection :fit]}
   {:label "Fill selected"
    :id "fill-selected"
    :action [::frame.e/focus-selection :fill]}])

(defn zoom-menu
  []
  [:> DropdownMenu/Root
   [:> DropdownMenu/Trigger
    {:class "button flex items-center justify-center overlay px-2 font-mono rounded-sm"
     :side "top"}
    [:div.flex.items-center
     [ui/icon "chevron-up"]]]
   [:> DropdownMenu/Portal
    [:> DropdownMenu/Content
     {:class "menu-content rounded-sm"
      :side "top"
      :align "end"}
     (for [item zoom-options]
       ^{:key (:id item)} [ui/dropdown-menu-item item])
     [:> DropdownMenu/Arrow {:class "menu-arrow"}]]]])

(def view-radio-buttons
  [{:title "Timeline"
    :active [::app.s/panel-visible? :timeline]
    :icon "animation"
    :class "hidden sm:inline-block shrink-0"
    :action [::app.e/toggle-panel :timeline]}
   {:title "Grid"
    :active [::app.s/grid]
    :icon "grid"
    :class "shrink-0"
    :action [::app.e/toggle-grid]}
   {:title "Rulers"
    :active [::ruler.s/visible?]
    :icon "ruler-combined"
    :class "shrink-0"
    :action [::ruler.e/toggle-visible]}
   {:title "History"
    :active [::app.s/panel-visible? :history]
    :icon "history"
    :class "hidden sm:inline-block shrink-0"
    :action [::app.e/toggle-panel :history]}
   {:title "XML"
    :class "hidden sm:inline-block shrink-0"
    :active [::app.s/panel-visible? :xml]
    :icon "code"
    :action [::app.e/toggle-panel :xml]}])

(defn set-zoom
  [e v]
  (let [new-v (-> (.. e -target -value) js/parseFloat (/ 100))]
    (if (js/isNaN new-v)
      (set! (.. e -target -value) v)
      (rf/dispatch [::frame.e/set-zoom new-v]))))

(defn zoom-decimal-points
  [zoom]
  (condp > zoom
    1  2
    10 1
    0))

(defn zoom-input
  [zoom]
  (let [value (.toFixed (* 100 zoom) (zoom-decimal-points zoom) 2)]
    [:input.form-element.overlay.text-right
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
      :on-key-down #(keyb/input-key-down-handler! % value set-zoom % value)
      :on-wheel #(rf/dispatch (if (pos? (.-deltaY %))
                                [::frame.e/zoom-out]
                                [::frame.e/zoom-in]))}]))

(defn zoom-button-group
  []
  (let [zoom @(rf/subscribe [::document.s/zoom])]
    [:div.button-group
     [:button.button.overlay.px-2.font-mono.rounded
      {:disabled (<= zoom 0.01)
       :title "Zoom out"
       :on-click #(rf/dispatch [::frame.e/zoom-out])}
      [ui/icon "minus"]]

     [:button.button.overlay.px-2.font-mono.rounded
      {:disabled (>= zoom 100)
       :title "Zoom in"
       :on-click #(rf/dispatch [::frame.e/zoom-in])}
      [ui/icon "plus"]]
     [:div.flex.hidden
      {:class "md:flex"}
      [zoom-input zoom]
      [:div.pr-2.overlay.flex.items-center "%"]]
     [zoom-menu]]))

(defn root []
  (let [help-message @(rf/subscribe [::tool.s/help])
        loading @(rf/subscribe [::worker.s/loading])
        fill @(rf/subscribe [::document.s/fill])
        stroke @(rf/subscribe [::document.s/stroke])
        get-hex #(:hex (js->clj % :keywordize-keys true))]
    [:div.toolbar.bg-primary.mt-px.relative
     [:div.flex
      [ui/color-picker
       {:color fill
        :on-change-complete #(rf/dispatch [::element.e/set-attr :fill (get-hex %)])
        :on-change #(rf/dispatch [::document.e/preview-attr :fill (get-hex %)])}

       [:button.button.color-rect
        {:style {:background fill}}]]

      [:button.icon-button
       {:title (t [:color/swap "Swap fill with stroke"])
        :style {:width "21px" :background "transparent"}
        :on-click #(rf/dispatch [::document.e/swap-colors])}
       [ui/icon "swap-horizontal"]]
      ;; REVIEW: Can we replace alignOffset with collisionBoundary?
      [ui/color-picker
       {:color stroke
        :on-change-complete #(rf/dispatch [::element.e/set-attr :stroke (get-hex %)])
        :on-change #(rf/dispatch [::document.e/preview-attr :stroke (get-hex %)])
        :align-offset -54}
       [:button.button.color-rect.relative
        {:style {:background stroke}}
        [:div.color-rect.bg-primary.absolute
         {:style {:width "13px"
                  :height "13px"
                  :bottom "9px"
                  :right "9px"}}]]]]
     [:div.grow
      [:div.px-1.hidden.gap-1.flex-wrap.leading-none.truncate
       {:class "2xl:flex"
        :style {:max-height "var(--button-size)"}}
       help-message]]
     (when loading
       [:button.icon-button
        [ui/loading-indicator]])
     (into [:<>]
           (map (fn [{:keys [title active icon action class]}]
                  [ui/radio-icon-button icon @(rf/subscribe active)
                   {:title title
                    :class class
                    :on-click #(rf/dispatch action)}])
                view-radio-buttons))
     [snap.v/root]
     [zoom-button-group]
     [coordinates]
     [timeline.v/time-bar]]))
