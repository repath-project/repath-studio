(ns renderer.timeline.views
  (:require
   ["@radix-ui/react-switch" :as Switch]
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   ["react" :as react]
   [re-frame.core :as rf]
   [reagent.core :as ra]
   [renderer.components :as comp]))

(defn toolbar
  [editor-ref]
  (let [time @(rf/subscribe [:timeline/time-formatted])
        paused? @(rf/subscribe [:timeline/paused?])
        grid-snap? @(rf/subscribe [:timeline/grid-snap?])
        guide-snap? @(rf/subscribe [:timeline/guide-snap?])]
    [:div.toolbar.level-1
     (if paused?
       [comp/icon-button "play" {:on-click #(.play (.-current editor-ref))}]
       [comp/icon-button "pause" {:on-click #(.pause (.-current editor-ref))}])
     [:span.p-2.font-mono time]
     [:span.v-divider]
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
       "Line snap"]
      [:> Switch/Root
       {:class "switch-root"
        :id "guide-snap"
        :default-checked guide-snap?
        :on-checked-change #(rf/dispatch [:timeline/set-guide-snap %])}
       [:> Switch/Thumb {:class "switch-thumb"}]]]]))

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
              guide-snap? @(rf/subscribe [:timeline/guide-snap?])]
          [:div
           [toolbar ref]
           [:> Timeline {:editor-data data
                         :effects effects
                         :ref ref
                         :grid-snap grid-snap?
                         :drag-line guide-snap?
                         :auto-scroll true
                         :on-click-action #(rf/dispatch [:element/select (keyword (.. %2 -action -id))])}]]))})))
