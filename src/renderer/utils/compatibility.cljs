(ns renderer.utils.compatibility
  (:require
   [malli.experimental :as mx]
   [renderer.document.db :as document.db]
   [renderer.utils.migrations :as migrations]))

;; https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
(def ver-regex #"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

(def ver
  [:tuple int? int? int?])

(mx/defn version->table :- [:maybe ver]
  [s :- [:maybe [:re ver-regex]]]
  (->> s
       (str)
       (re-find ver-regex)
       (rest)
       (take 3)
       (mapv js/parseInt)))

(mx/defn requires-migration? :- boolean?
  [document :- map?, [m-major m-minor m-patch] :- ver]
  (let [[major minor patch] (version->table (:version document))]
    (or (< major m-major)
        (and (= major m-major)
             (< minor m-minor))
        (and (= major m-major)
             (= minor m-minor)
             (< patch m-patch)))))

(mx/defn migrate-document :- document.db/persisted
  [document :- map?]
  (reduce (fn [document [m-ver f]]
            (cond-> document
              (requires-migration? document m-ver) f)) document migrations/migrations))

