(ns renderer.timeline.views
  (:require
   ["@radix-ui/react-switch" :as Switch]
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]))

(defn snap-controls
  []
  (let [grid-snap? @(rf/subscribe [:timeline/grid-snap?])
        guide-snap? @(rf/subscribe [:timeline/guide-snap?])]
    [:<>
     [:span.inline-flex.items-center
      [:label
       {:for "grid-snap"
        :style {:background "transparent"
                :height "auto"}}
       "Grid snap"]
      [:> Switch/Root
       {:class "switch-root"
        :id "grid-snap"
        :default-checked grid-snap?
        :on-checked-change #(rf/dispatch [:timeline/set-grid-snap %])}
       [:> Switch/Thumb {:class "switch-thumb"}]]]
     [:span.inline-flex.items-center
      [:label
       {:for "guide-snap"
        :style {:background "transparent"
                :height "auto"}}
       "Guide snap"]
      [:> Switch/Root
       {:class "switch-root"
        :id "guide-snap"
        :default-checked guide-snap?
        :on-checked-change #(rf/dispatch [:timeline/set-guide-snap %])}
       [:> Switch/Thumb {:class "switch-thumb"}]]]]))

(defn toolbar
  [editor-ref]
  (let [time @(rf/subscribe [:timeline/time])
        time-formatted @(rf/subscribe [:timeline/time-formatted])
        paused? @(rf/subscribe [:timeline/paused?])
        replay? @(rf/subscribe [:timeline/replay?])
        end @(rf/subscribe [:timeline/end])
        timeline? @(rf/subscribe [:panel/visible? :timeline])]
    [:div.toolbar.level-1.mb-px
     [:div.flex-1.flex
      [comp/icon-button "go-to-start" {:on-click #(.setTime (.-current editor-ref) 0)
                                       :disabled (zero? time)}]
      (if paused?
        [comp/icon-button "play" {:on-click #(.play (.-current editor-ref) #js {:autoEnd true})}]
        [comp/icon-button "pause" {:on-click #(.pause (.-current editor-ref))}])
      [comp/icon-button "go-to-end" {:on-click #(.setTime (.-current editor-ref) end)
                                     :disabled (>= time end)}]
      [comp/radio-icon-button
       {:title "Replay"
        :active? replay?
        :icon "refresh"
        :action #(rf/dispatch [:timeline/toggle-replay])}]
      [:span.p-2.font-mono time-formatted]
      (when timeline?
        [:<>
         [:span.v-divider]
         [snap-controls]])]
     [comp/toggle-icon-button
      {:active? (not timeline?)
       :active-icon "chevron-up"
       :active-text "Show timeline"
       :inactive-icon "times"
       :inactive-text "Hide timeline"
       :action #(rf/dispatch [:panel/toggle :timeline])}]]))

(defn root
  []
  (let [ref (react/createRef)]
    (ra/create-class
     {:component-did-mount
      (fn []
        (rf/dispatch [:timeline/pause])
        (rf/dispatch [:timeline/set-time 0])
        (doseq
         [[e f]
          [["play" #(rf/dispatch-sync [:timeline/play])] ;; Prevent navigation
           ["paused" #(rf/dispatch-sync [:timeline/pause])]
           ["ended" #(when @(rf/subscribe [:timeline/replay?])
                       (.setTime (.-current ref) 0)
                       (.play (.-current ref) #js {:autoEnd true}))]
           ["afterSetTime" #(rf/dispatch-sync [:timeline/set-time (.-time %)])]
           ["setTimeByTick" #(rf/dispatch-sync [:timeline/set-time (.-time %)])]]]
          (.on (.-listener (.-current ref)) e f)))

      :component-will-unmount
      #(.offAll (.-listener (.-current ref)))

      :reagent-render
      (fn []
        (let [data @(rf/subscribe [:timeline/rows])
              effects @(rf/subscribe [:timeline/effects])
              grid-snap? @(rf/subscribe [:timeline/grid-snap?])
              guide-snap? @(rf/subscribe [:timeline/guide-snap?])
              timeline? @(rf/subscribe [:panel/visible? :timeline])]
          [:div
           [toolbar ref]
           [:> Timeline {:style {:height (if timeline? "200px" 0)}
                         :editor-data data
                         :effects effects
                         :ref ref
                         :grid-snap grid-snap?
                         :drag-line guide-snap?
                         :auto-scroll true
                         :on-click-action #(rf/dispatch [:element/select (keyword (.. %2 -action -id))])}]]))})))
