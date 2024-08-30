(ns renderer.history.db
  (:require
   [renderer.element.db :as element.db]
   [renderer.utils.math :as math]))

(def state
  [:map {:closed true}
   [:explanation string?]
   [:timestamp number?]
   [:index [:or pos-int? zero?]]
   [:id uuid?]
   [:elements element.db/elements]
   [:parent {:optional true} uuid?]
   [:children [:vector uuid?]]])

(def history
  [:map {:default {} :closed true}
   [:zoom {:optional true :default 0.5} number?]
   [:translate {:optional true} math/vec2d]
   [:position {:optional true} uuid?]
   [:states {:default {}} [:map-of uuid? state]]])
