(ns renderer.timeline.views
  (:require
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   [re-frame.core :as rf]
   [reagent.core :as r]
   [renderer.components :as comp]))

(defn root
  []
  (let [data @(rf/subscribe [:timeline/rows])
        effects @(rf/subscribe [:timeline/effects])
        time @(rf/subscribe [:timeline/time-formatted])
        paused? @(rf/subscribe [:timeline/paused?])
        engine (atom nil)]
    [:div
     [:div.toolbar.level-1
      (if paused?
        [comp/icon-button "play" {:on-click #(.play @engine)}]
        [comp/icon-button "pause" {:on-click #(.pause @engine)}])
      [:span.p-2 time]]
     [:> Timeline {:editor-data data
                   :effects effects
                   :ref (fn [this]
                          (when this
                            (reset! engine this)
                            (doseq
                             [[e f]
                              [["play" #(rf/dispatch [:timeline/play])] ;; Prevent navigation
                               ["paused" #(rf/dispatch [:timeline/pause])]
                               ["afterSetTime" #(rf/dispatch-sync [:timeline/set-time (.-time %)])]
                               ["setTimeByTick" #(rf/dispatch-sync [:timeline/set-time (.-time %)])]]]
                              (.on (.-listener this) e f))))
                  ;;  :onCursorDrag #(rf/dispatch [:timeline/set-time %])
                  ;;  :onCursorDragStart #(rf/dispatch [:timeline/pause])
                   :onClickAction #(rf/dispatch [:element/select (keyword (.. %2 -action -id))])}]]))
