(ns renderer.utils.compatibility
  (:require
   [malli.experimental :as mx]
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

(mx/defn version->vec :- Version
  [s :- string?]
  (->> (re-find ver-regex s)
       (rest)
       (take 3)
       (mapv js/parseInt)))

(mx/defn requires-migration? :- boolean?
  [document-version :- Version, migration-version :- Version]
  (let [[m-major m-minor m-patch] migration-version
        [d-major d-minor d-patch] document-version]
    (or (< d-major m-major)
        (and (= d-major m-major)
             (< d-minor m-minor))
        (and (= d-major m-major)
             (= d-minor m-minor)
             (< d-patch m-patch)))))

(mx/defn migrate-document :- PersistedDocument
  [document :- map?]
  (reduce (fn [document [migration-version migration-f]]
            (cond-> document
              (:or (not (:version document))
                   (requires-migration? (version->vec (:version document)) migration-version))
              migration-f))
          document
          migrations/migrations))

