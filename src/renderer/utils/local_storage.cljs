(ns renderer.utils.local-storage
  "Interceptor that persists part of app-db to local storage
   https://github.com/akiroz/re-frame-storage"
  (:require
   [akiroz.re-frame.storage :refer [persist-db-keys ->store]]))

(def store-key :repath)

(def persistent-keys
  "Top level keys that should be persisted"
  [:theme
   :panels
   :grid-visible?
   :rulers-visible?
   :recent
   :document-tabs
   :documents
   :active-document])

(def persist
  (persist-db-keys store-key persistent-keys))

(defn ->store!
  [db]
  (->store store-key (select-keys db persistent-keys)))
