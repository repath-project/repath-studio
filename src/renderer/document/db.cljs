(ns renderer.document.db
  (:require
   [malli.core :as m]
   [renderer.element.db :as element.db]
   [renderer.history.db :as history.db]))

(def zoom
  [:fn {:error/fn (fn [{:keys [value]} _] (str value ", should be between 0.1 and 1000"))}
   (fn [x] (and (number? x) (<= 0.01 x 100)))])

(def document
  [:map
   [:id keyword?]
   [:title {:min 1} string?]
   [:hovered-ids {:default #{}} [:set keyword?]]
   [:collapsed-ids {:default #{}} [:set keyword?]]
   [:ignored-ids {:default #{}} [:set keyword?]]
   [:fill {:default "white"} string?]
   [:stroke {:default "black"} string?]
   [:zoom {:default 1} zoom]
   [:rotate {:default 0} number?]
   [:history history.db/history]
   [:pan {:default [0 0]} [:tuple number? number?]]
   [:elements element.db/elements]
   [:focused? {:optional true} boolean?]])

(def valid? (m/validator document))


