(ns renderer.history.db
  (:require
   [renderer.element.db]))

(def state
  [:map
   [:explenation string?]
   [:timestamp double?]
   [:index integer?]
   [:id keyword?]
   [:elements [:map-of keyword? renderer.element.db/element]]
   [:parent keyword?]
   [:children [:vector keyword?]]])

(def history
  [:map
   [:zoom double?]
   [:position keyword?]
   [:states [:map-of keyword? state]]])
