(ns renderer.utils.map
  (:require
   [clojure.set :as set]
   [malli.experimental :as mx]))

(mx/defn merge-common-with :- map?
  "Equivelent to merge-with for common keys across all maps."
  [f :- fn? & maps :- [:* map?]]
  (let [common-keys (apply set/intersection (map (comp set keys) maps))]
    (->> (apply merge-with f (map #(select-keys % common-keys) maps))
         (into {}))))

(mx/defn remove-nils :- map?
  "Removes nil values from maps (should be used sparingly)."
  [m :- map?]
  (->> m
       (remove (comp nil? val))
       (into {})))
