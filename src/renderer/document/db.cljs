(ns renderer.document.db
  (:require
   [renderer.element.db :as element.db]
   [renderer.history.db :as history.db]))

(def document
  [:map
   [:key keyword?]
   [:title [string? {:min 1}]]
   [:hovered-keys [:set {:default #{}} keyword?]]
   [:collapsed-keys [:set {:default #{}} keyword?]]
   [:ignored-keys [:set {:default #{}} keyword?]]
   [:fill [string? {:default "white"}]]
   [:stroke [string? {:default "black"}]]
   [:zoom [double? {:default 1}]]
   [:rotate [double? {:default 0}]]
   [:history history.db/history]
   [:pan [:tuple {:default [0 0]} double? double?]]
   [:elements element.db/elements]])

(def documents
  [:map-of {:default {}} keyword? document])
