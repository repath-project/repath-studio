(ns renderer.document.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.element.db :as element.db]
   [renderer.history.db :as history.db]))

(def document
  [:map {:closed true}
   [:id uuid?]
   [:title {:min 1} string?]
   [:path {:optional true} [:maybe string?]]
   [:save {:optional true} uuid?]
   [:version {:optional true} string?]
   [:hovered-ids {:default #{}} [:set [:or keyword? uuid?]]]
   [:collapsed-ids {:default #{}} [:set uuid?]]
   [:ignored-ids {:default #{}} [:set [:or keyword? uuid?]]]
   [:fill {:default "white"} string?]
   [:stroke {:default "black"} string?]
   [:zoom {:default 1} [:and number? [:>= 0.01] [:<= 100]]]
   [:rotate {:default 0} number?]
   [:history history.db/history]
   [:temp-element {:optional true} any?] ; REVIEW
   [:pan {:default [0 0]} [:tuple number? number?]]
   [:elements element.db/elements]
   [:focused? {:optional true} boolean?]])

(def valid? (m/validator document))

(def explain (m/explainer document))

(def default (m/decode document {} mt/default-value-transformer))

