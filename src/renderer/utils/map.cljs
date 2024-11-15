(ns renderer.utils.map
  (:require
   [clojure.set :as set]
   [malli.core :as m]))

(m/=> merge-common-with [:-> ifn? [:* map?] map?])
(defn merge-common-with
  "Equivelent to merge-with for common keys across all maps."
  [f & maps]
  (let [common-keys (apply set/intersection (map (comp set keys) maps))]
    (->> (apply merge-with f (map #(select-keys % common-keys) maps))
         (into {}))))

(m/=> remove-nils [:-> map? map?])
(defn remove-nils
  "Removes nil values from maps (should be used sparingly)."
  [m]
  (->> m
       (remove (comp nil? val))
       (into {})))
