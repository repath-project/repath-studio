(ns renderer.history.db
  (:require
   [renderer.element.db :as element.db]))

(def state
  [:map
   [:explanation string?]
   [:timestamp double?]
   [:index [:or pos-int? zero?]]
   [:id keyword?]
   [:elements [:map-of keyword? element.db/element]]
   [:parent {:optional true} keyword?]
   [:children [:vector keyword?]]])

(def history
  [:map {:default {}}
   [:zoom {:optional true} [double? {:default 0.5}]]
   [:position keyword?]
   [:states [:map-of {:default {}} keyword? state]]])
