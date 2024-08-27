(ns renderer.document.db
  (:require
   [malli.core :as m]
   [renderer.element.db :as element.db]
   [renderer.history.db :as history.db]))

(def document
  [:map {:closed true}
   [:id keyword?]
   [:title {:min 1} string?]
   [:path {:optional true} string?]
   [:save {:optional true} keyword?]
   [:version {:optional true} string?]
   [:hovered-ids {:default #{}} [:set keyword?]]
   [:collapsed-ids {:default #{}} [:set keyword?]]
   [:ignored-ids {:default #{}} [:set keyword?]]
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


