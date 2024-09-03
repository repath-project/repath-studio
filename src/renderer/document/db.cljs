(ns renderer.document.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [malli.util :as mu]
   [renderer.element.db :refer [elements]]
   [renderer.history.db :refer [history]]))

(def document
  [:map {:closed true}
   [:id {:optional true} uuid?]
   [:title {:optional true :min 1} string?]
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
   [:history history]
   [:temp-element {:optional true} any?] ; REVIEW
   [:pan {:default [0 0]} [:tuple number? number?]]
   [:elements elements]
   [:focused? {:optional true} boolean?]])

(def persisted (mu/select-keys document [:id :title :path :save :version :elements]))

(def valid? (m/validator document))

(def explain (m/explainer document))

(def default (m/decode document {} mt/default-value-transformer))

