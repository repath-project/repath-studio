(ns renderer.document.db
  (:require
   [malli.core :as m]
   [malli.transform :as mt]
   [renderer.element.db :refer [Element]]
   [renderer.history.db :refer [History]]
   [renderer.menubar.filters :as filters]
   [renderer.utils.math :refer [Vec2D]]))

(def ZoomFactor
  [:and number? [:>= 0.01] [:<= 100]])

(def A11yFilter
  (into [:enum] (map :id filters/accessibility)))

(def Document
  [:map {:closed true}
   [:id {:optional true :persist true} uuid?]
   [:title {:optional true :min 1 :persist true} string?]
   [:path {:optional true :persist true} [:maybe string?]]
   [:save {:optional true} uuid?]
   [:version {:optional true :persist true} string?]
   [:hovered-ids {:default #{}} [:set [:or keyword? uuid?]]]
   [:collapsed-ids {:default #{}} [:set uuid?]]
   [:ignored-ids {:default #{}} [:set [:or keyword? uuid?]]]
   [:zoom {:default 1} ZoomFactor]
   [:rotate {:default 0} number?]
   [:history {:optional true} History]
   [:temp-element {:optional true} Element]
   [:pan {:default [0 0]} Vec2D]
   [:elements {:default {} :persist true} [:map-of uuid? Element]]
   [:focused {:optional true} boolean?]
   [:filter {:optional true} A11yFilter]
   [:attrs {:default {:fill "white" :stroke "black"}} [:map-of keyword? string?]]])

(def PersistedDocument
  (->> Document
       (m/children)
       (filter (comp :persist second))
       (into [:map])))

(def valid? (m/validator Document))

(def explain (m/explainer Document))

(def default (m/decode Document {} mt/default-value-transformer))

