(ns renderer.history.db
  (:require
   [renderer.db :refer [Vec2]]
   [renderer.element.db :refer [Element ElementId]]))

(def Explanation [:* any?])

(def HistoryId uuid?)

(def HistoryIndex [:or pos-int? zero?])

(def HistoryState
  [:map {:closed true}
   [:explanation Explanation]
   [:timestamp number?]
   [:index HistoryIndex]
   [:id HistoryId]
   [:elements {:optional true} [:map-of ElementId Element]]
   [:parent {:optional true} HistoryId]
   [:children [:vector HistoryId]]])

(def History
  [:map {:closed true}
   [:zoom {:optional true} number?]
   [:translate {:optional true} Vec2]
   [:position {:optional true} HistoryId]
   [:states {:default {}} [:map-of uuid? HistoryState]]])
