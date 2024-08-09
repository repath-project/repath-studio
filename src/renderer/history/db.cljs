(ns renderer.history.db
  (:require
   [renderer.element.db]))

(def state
  [:map
   [:explanation string?]
   [:timestamp double?]
   [:index integer?]
   [:id keyword?]
   [:elements [:map-of keyword? renderer.element.db/element]]
   [:parent {:optional true} keyword?]
   [:children [:vector keyword?]]])

(def history
  [:map
   [:zoom double?]
   [:position keyword?]
   [:states [:map-of keyword? state]]])
