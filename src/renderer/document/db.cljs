(ns renderer.document.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.element.db :refer [Element]]
   [renderer.history.db :refer [History]]
   [renderer.menubar.filters :refer [A11yFilter]]
   [renderer.utils.math :refer [Vec2]]))

(def ZoomFactor
  [:and number? [:>= 0.01] [:<= 100]])

(def Document
  [:map {:closed true}
   [:id {:optional true
         :persist true} uuid?]
   [:title {:optional true
            :min 1
            :persist true} string?]
   [:path {:optional true
           :persist true} [:maybe string?]]
   [:saved-history-id {:optional true} uuid?]
   [:version {:optional true
              :persist true} string?]
   [:hovered-ids {:default #{}} [:set [:or keyword? uuid?]]]
   [:collapsed-ids {:default #{}} [:set uuid?]]
   [:ignored-ids {:default #{}} [:set [:or keyword? uuid?]]]
   [:zoom {:default 1} ZoomFactor]
   [:rotate {:default 0} number?]
   [:history {:optional true} History]
   [:pan {:default [0 0]} Vec2]
   [:elements {:default {}
               :persist true} [:map-of uuid? Element]]
   [:centered {:optional true} boolean?]
   [:filter {:optional true} A11yFilter]
   [:attrs {:default {:fill "white"
                      :stroke "black"}} [:map-of keyword? string?]]
   [:preview-label {:optional true} string?]
   [:file-handle {:optional true} any?]])

(def PersistedDocument
  (->> Document
       (m/children)
       (filter (comp :persist second))
       (into [:map {:closed true}])))

(def SaveInfo
  (->> Document
       (m/children)
       (filter (comp #(some #{%} config/save-excluded-keys) first))
       (into [:map {:closed true}])))

(def valid? (m/validator Document))

(def explain (m/explainer Document))

(def default (m/decode Document {} m.transform/default-value-transformer))
