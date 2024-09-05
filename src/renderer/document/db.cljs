(ns renderer.document.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [malli.util :as mu]
   [renderer.element.db :refer [Element]]
   [renderer.history.db :refer [History]]
   [renderer.utils.math :refer [Vec2D]]))

(def Document
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
   [:history History]
   [:temp-element {:optional true} any?] ; REVIEW
   [:pan {:default [0 0]} Vec2D]
   [:elements [:map-of {:default {}} uuid? Element]]
   [:focused? {:optional true} boolean?]])

(def Persisted (mu/select-keys Document [:id :title :path :save :version :elements]))

(def valid? (m/validator Document))

(def explain (m/explainer Document))

(def default (m/decode Document {} mt/default-value-transformer))

