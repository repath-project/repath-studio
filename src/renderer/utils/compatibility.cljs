(ns renderer.utils.compatibility
  (:require
   [malli.experimental :as mx]
   [renderer.utils.migrations :as migrations]))

;; https://semver.org/#is-there-a-suggested-regular-expression-regex-to-check-a-semver-string
(def ver-regex #"(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-((?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(?:\.(?:0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(?:\+([0-9a-zA-Z-]+(?:\.[0-9a-zA-Z-]+)*))?$")

(def ver
  [:map
   [:major [:or zero? pos-int?]]
   [:minor [:or zero? pos-int?]]
   [:patch [:or zero? pos-int?]]])

(mx/defn version->table :- [:maybe ver]
  [s :- [:maybe [:re ver-regex]]]
  (->> s
       str
       (re-find ver-regex)
       (rest)
       (map js/parseInt)
       (zipmap [:major :minor :patch])))

(mx/defn compatible? :- boolean?
  [& versions :- [:* [:maybe [:re ver-regex]]]]
  (apply = (map #(-> % version->table (select-keys [:major :minor])) versions)))

(mx/defn migrate-document :- map?
  [document :- map?]
  (let [{:keys [major minor]} (version->table (:version document))]
    (reduce (fn [document [[m-major m-minor] f]]
              (cond-> document
                (or (< major m-major)
                    (and (= major m-major)
                         (< minor m-minor)))
                f)) document migrations/migrations)))
