(ns renderer.history.db
  (:require
   [renderer.element.db :refer [elements]]
   [renderer.utils.math :refer [vec2d]]))

(def state
  [:map {:closed true}
   [:explanation string?]
   [:timestamp number?]
   [:index [:or pos-int? zero?]]
   [:id uuid?]
   [:elements elements]
   [:parent {:optional true} uuid?]
   [:children [:vector uuid?]]])

(def history
  [:map {:default {} :closed true}
   [:zoom {:optional true :default 0.5} number?]
   [:translate {:optional true} vec2d]
   [:position {:optional true} uuid?]
   [:states {:default {}} [:map-of uuid? state]]])
