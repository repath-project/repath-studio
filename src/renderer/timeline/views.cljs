(ns renderer.timeline.views
  (:require
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   ["@radix-ui/react-select" :as Select]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]
   [renderer.element.events :as-alias element.e]
   [renderer.timeline.events :as-alias timeline.e]
   [renderer.timeline.subs :as-alias timeline.s]))

(def speed-options
  [{:id :0.25
    :value 0.25
    :label "0.25x"}
   {:id :0.5
    :value 0.5
    :label "0.5x"}
   {:id :normal
    :value 1
    :label "1x"}
   {:id :1.5
    :value 1.5
    :label "1.5x"}
   {:id :2
    :value 2
    :label "2x"}])

(defn speed-select
  [editor-ref]
  (let [speed @(rf/subscribe [::timeline.s/speed])]
    [:div.inline-flex.items-center
     [:label "Speed"]
     [:> Select/Root
      {:value speed
       :onValueChange #(.setPlayRate (.-current editor-ref) %)}
      [:> Select/Trigger
       {:class "select-trigger"
        :aria-label "No a11y filter"}
       [:> Select/Value {:placeholder "Filter"}
        [:div.flex.gap-1.justify-between.items-center
         {:style {:min-width "50px"}}
         [:span (str speed "x")]
         [:> Select/Icon {:class "select-icon"}
          [comp/icon "chevron-down" {:class "icon small"}]]]]]
      [:> Select/Portal
       [:> Select/Content
        {:class "menu-content rounded select-content"
         :style {:min-width "auto"}}
        [:> Select/ScrollUpButton {:class "select-scroll-button"}
         [comp/icon "chevron-up"]]
        [:> Select/Viewport {:class "select-viewport"}
         [:> Select/Group
          (for [{:keys [id value label]} speed-options]
            ^{:key id}
            [:> Select/Item
             {:value value
              :class "menu-item select-item"}
             [:> Select/ItemText label]])]]
        [:> Select/ScrollDownButton
         {:class "select-scroll-button"}
         [comp/icon "chevron-down"]]]]]]))

(defn snap-controls
  []
  (let [grid-snap? @(rf/subscribe [::timeline.s/grid-snap?])
        guide-snap? @(rf/subscribe [::timeline.s/guide-snap?])]
    [:div.grow
     [comp/switch
      {:id "grid-snap"
       :label "Grid snap"
       :default-checked? grid-snap?
       :on-checked-change #(rf/dispatch [::timeline.e/set-grid-snap %])}]
     [comp/switch
      {:id "guide-snap"
       :label "Guide snap"
       :default-checked? guide-snap?
       :on-checked-change #(rf/dispatch [::timeline.e/set-guide-snap %])}]]))

(defn toolbar
  [timeline-ref]
  (let [time @(rf/subscribe [::timeline.s/time])
        time-formatted @(rf/subscribe [::timeline.s/time-formatted])
        paused? @(rf/subscribe [::timeline.s/paused?])
        replay? @(rf/subscribe [::timeline.s/replay?])
        end @(rf/subscribe [::timeline.s/end])]
    [:div.toolbar.bg-primary
     [comp/icon-button "go-to-start"
      {:on-click #(.setTime (.-current timeline-ref) 0)
       :disabled (zero? time)}]
     [comp/radio-icon-button
      {:title (if paused? "Play" "Pause")
       :class (when (pos? time) "border border-accent")
       :active? (not paused?)
       :icon (if paused? "play" "pause")
       :action #(if paused?
                  (.play (.-current timeline-ref) #js {:autoEnd true})
                  (.pause (.-current timeline-ref)))}]
     [comp/icon-button "go-to-end"
      {:on-click #(.setTime (.-current timeline-ref) end)
       :disabled (>= time end)}]
     [comp/radio-icon-button
      {:title "Replay"
       :active? replay?
       :icon "refresh"
       :action #(rf/dispatch [::timeline.e/toggle-replay])}]
     [speed-select timeline-ref]
     [:span.font-mono.px-2 time-formatted]
     [:span.v-divider]
     [snap-controls]
     [comp/icon-button "window-close"
      {:title "Hide timeline"
       :on-click #(rf/dispatch [:toggle-panel :timeline])}]]))

(defn register-listeners
  [timeline-ref]
  (doseq
   [[e f]
    [["play" #(rf/dispatch-sync [::timeline.e/play])] ;; Prevent navigation
     ["paused" #(rf/dispatch-sync [::timeline.e/pause])]
     ["ended" #(do (.setTime (.-current timeline-ref) 0)
                   (when @(rf/subscribe [::timeline.e/replay?])
                     (.play (.-current timeline-ref) #js {:autoEnd true})))]
     ["afterSetTime" #(rf/dispatch-sync [::timeline.e/set-time (.-time %)])]
     ["setTimeByTick" #(rf/dispatch-sync [::timeline.e/set-time (.-time %)])]
     ["afterSetPlayRate" #(rf/dispatch [::timeline.e/set-speed (.-rate %)])]]]
    (.on (.-listener (.-current timeline-ref)) e f)))

(defn custom-renderer
  [action _row]
  (ra/as-element
   [:span (.-name action)]))

(defn timeline
  [timeline-ref]
  (let [data @(rf/subscribe [::timeline.s/rows])
        effects @(rf/subscribe [::timeline.s/effects])
        grid-snap? @(rf/subscribe [::timeline.s/grid-snap?])
        guide-snap? @(rf/subscribe [::timeline.s/guide-snap?])]
    [:> Timeline
     {:editor-data data
      :effects effects
      :ref timeline-ref
      :grid-snap grid-snap?
      :drag-line guide-snap?
      :auto-scroll true
      :getActionRender custom-renderer
      :on-click-action #(rf/dispatch [::element.e/select (keyword (.. %2 -action -id))])}]))

(defn time-bar
  []
  (let [time @(rf/subscribe [::timeline.s/time])
        end @(rf/subscribe [::timeline.s/end])
        timeline? @(rf/subscribe [:panel-visible? :timeline])]
    [:div.h-px.block
     {:style {:width (str (* (/ time end) 100) "%")
              :background (when-not (or (zero? time) (zero? end) timeline?)
                            "var(--accent)")}}]))

(defn root
  []
  (let [timeline-ref (react/createRef)]
    (ra/create-class
     {:component-did-mount
      (fn []
        (rf/dispatch [::timeline.e/pause])
        (rf/dispatch [::timeline.e/set-time 0])
        (register-listeners timeline-ref))

      :component-will-unmount
      #(.offAll (.-listener (.-current timeline-ref)))

      :reagent-render
      (fn []
        [:div.flex-col.h-full
         [toolbar timeline-ref]
         [timeline timeline-ref]])})))
