(ns renderer.history.db
  (:require
   [renderer.element.db :refer [Element]]
   [renderer.utils.math :refer [Vec2]]))

(def Explanation [:* any?])

(def HistoryState
  [:map {:closed true}
   [:explanation Explanation]
   [:timestamp number?]
   [:index [:or pos-int? zero?]]
   [:id uuid?]
   [:elements {:optional true} [:map-of uuid? Element]]
   [:parent {:optional true} uuid?]
   [:children [:vector uuid?]]])

(def History
  [:map {:closed true}
   [:zoom {:optional true} number?]
   [:translate {:optional true} Vec2]
   [:position {:optional true} uuid?]
   [:states {:default {}} [:map-of uuid? HistoryState]]])
