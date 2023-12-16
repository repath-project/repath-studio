(ns renderer.timeline.views
  (:require
   ["@xzdarcy/react-timeline-editor" :refer [Timeline]]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.components :as comp]))

(defn time-rendered
  [time]
  (let [float (-> time (rem 1) (* 100) str js/parseInt str (.padStart 2 "0") (str/replace "0." "")) 
        min (-> time (/ 60) str js/parseInt str (.padStart 2 "0"))
        sec (-> time (rem 60) str js/parseInt str (.padStart 2 "0"))]
    (str min ":"  sec ":" float)))

(defn root
  []
  (let [data @(rf/subscribe [:timeline/rows])
        effects @(rf/subscribe [:timeline/effects])
        time @(rf/subscribe [:timeline/time])
        paused? @(rf/subscribe [:timeline/paused?])]
    [:div
     [:div.toolbar.level-1
      (if paused?
        [comp/icon-button "play" {:on-click #(rf/dispatch [:timeline/pause])}]
        [comp/icon-button "pause" {:on-click #(rf/dispatch [:timeline/play])}])
      [:span.p-2 [time-rendered time]]]
     [:> Timeline {:editor-data data
                   :effects effects
                   :onCursorDrag #(rf/dispatch [:timeline/set-time %1])
                   :onCursorDragStart #(rf/dispatch [:timeline/pause])
                   :onClickAction #(rf/dispatch [:element/select (keyword (.. %2 -action -id))])}]]))
