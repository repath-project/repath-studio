(ns renderer.utils.compatibility
  (:require
   [malli.core :as m]
   [renderer.document.db :refer [PersistedDocument]]
   [renderer.utils.migrations :as migrations]))

;; https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
(def ver-regex
  #"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

(def Version
  [:tuple
   [number? {:title "major"}]
   [number? {:title "minor"}]
   [number? {:title "patch"}]])

(m/=> version->vec [:-> string? Version])
(defn version->vec
  [s]
  (->> (re-find ver-regex s)
       (rest)
       (take 3)
       (mapv js/parseInt)))

(m/=> requires-migration? [:-> Version Version boolean?])
(defn requires-migration?
  [document-version migration-version]
  (let [[m-major m-minor m-patch] migration-version
        [d-major d-minor d-patch] document-version]
    (or (< d-major m-major)
        (and (= d-major m-major)
             (< d-minor m-minor))
        (and (= d-major m-major)
             (= d-minor m-minor)
             (< d-patch m-patch)))))

(m/=> migrate-document [:function
                        [:-> map? PersistedDocument]
                        [:-> map? PersistedDocument [:tuple Version fn?]]])
(defn migrate-document
  ([document]
   (reduce migrate-document document migrations/migrations))
  ([document [ver f]]
   (cond-> document
     (:or (not (:version document))
          (requires-migration? (version->vec (:version document)) ver))
     f)))

