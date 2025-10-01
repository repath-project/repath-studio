(ns renderer.document.db
  (:require
   [config :as config]
   [malli.core :as m]
   [malli.transform :as m.transform]
   [renderer.db :refer [Vec2]]
   [renderer.element.db :refer [Element ElementId]]
   [renderer.history.db :refer [History HistoryId]]
   [renderer.menubar.filters :refer [A11yFilter]]
   [renderer.tool.db :refer [HandleId]]))

(def ZoomFactor
  [:and number? [:>= 0.01] [:<= 100]])

(def DocumentId uuid?)

(def Document
  [:map {:closed true}
   [:id {:optional true
         :persist true} DocumentId]
   [:title {:optional true
            :min 1
            :persist true} string?]
   [:path {:optional true
           :persist true} string?]
   [:saved-history-id {:optional true} HistoryId]
   [:version {:optional true
              :persist true} string?]
   [:hovered-ids {:default #{}} [:set [:or HandleId ElementId]]]
   [:collapsed-ids {:default #{}} [:set ElementId]]
   [:ignored-ids {:default #{}} [:set [:or HandleId ElementId]]]
   [:zoom {:default 1} ZoomFactor]
   [:rotate {:default 0} number?]
   [:history {:optional true} History]
   [:pan {:default [0 0]} Vec2]
   [:elements {:default {}
               :persist true} [:map-of ElementId Element]]
   [:centered {:optional true} boolean?]
   [:filter {:optional true} A11yFilter]
   [:attrs {:default {:fill "white"
                      :stroke "black"}} [:map-of keyword? string?]]
   [:preview-label {:optional true} string?]])

(def PersistedDocument
  (->> Document
       (m/children)
       (filter (comp :persist second))
       (into [:map {:closed true}])))

(def SaveInfo
  (->> Document
       (m/children)
       (filter (comp #(some #{%} config/save-info-keys) first))
       (into [:map {:closed true}])))

(def valid? (m/validator Document))

(def explain (m/explainer Document))

(def default (m/decode Document
                       {}
                       m.transform/default-value-transformer))
