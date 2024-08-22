(ns renderer.utils.local-storage
  "Interceptor that persists part of app-db to local storage
   https://github.com/akiroz/re-frame-storage"
  (:require
   [akiroz.re-frame.storage :as rf.storage]
   [config :as config]))

(def persistent-keys
  "Top level keys that should be persisted"
  [:theme
   :panels
   :grid-visible?
   :rulers-visible?
   :recent
   :document-tabs
   :documents
   :active-document
   :version])

(def persist
  (rf.storage/persist-db-keys config/app-key persistent-keys))

(defn ->store!
  [db]
  (rf.storage/->store config/app-key (select-keys db persistent-keys)))

(defn clear!
  []
  (rf.storage/->store config/app-key {}))
