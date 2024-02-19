(ns renderer.utils.local-storage
  "Interceptor that persists part of app-db to local storage
   https://github.com/akiroz/re-frame-storage"
  (:require
   [akiroz.re-frame.storage :refer [persist-db-keys]]))

(def persistent-keys
  "Top level keys that should be persisted"
  [:theme :panel])

(def persist
  (persist-db-keys :repath persistent-keys))
