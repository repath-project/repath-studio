(ns renderer.history.db
  (:require
   [renderer.element.db :as element.db]
   [renderer.utils.math :as math]))

(def state
  [:map {:closed true}
   [:explanation string?]
   [:timestamp number?]
   [:index [:or pos-int? zero?]]
   [:id keyword?]
   [:elements element.db/elements]
   [:parent {:optional true} keyword?]
   [:children [:vector keyword?]]])

(def history
  [:map {:default {} :closed true}
   [:zoom {:optional true :default 0.5} number?]
   [:translate {:optional true} math/point]
   [:position {:optional true} keyword?]
   [:states {:default {}} [:map-of keyword? state]]])
