(ns renderer.history.db
  (:require
   [renderer.element.db :refer [Element]]
   [renderer.utils.math :refer [Vec2D]]))

(def State
  [:map {:closed true}
   [:explanation string?]
   [:timestamp number?]
   [:index [:or pos-int? zero?]]
   [:id uuid?]
   [:elements [:map-of uuid? Element]]
   [:parent {:optional true} uuid?]
   [:children [:vector uuid?]]])

(def History
  [:map {:default {} :closed true}
   [:zoom {:optional true :default 0.5} number?]
   [:translate {:optional true} Vec2D]
   [:position {:optional true} uuid?]
   [:states {:default {}} [:map-of uuid? State]]])
