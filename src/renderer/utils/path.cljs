(ns renderer.utils.path
  (:require
   ["paper" :refer [Path]]
   [malli.experimental :as mx]))

(def PathBooleanOperation
  [:enum :unite :intersect :subtract :exclude :divide])

(def PathManipulation
  [:enum :simplify :smooth :flatten :reverse])

(defn get-d
  [paper-path]
  (-> paper-path
      (.exportSVG)
      (.getAttribute "d")))

(mx/defn manipulate :- string?
  [path :- string?, manipulation :- PathManipulation]
  (let [path (Path. path)]
    (case manipulation
      :simplify (.simplify path)
      :smooth (.smooth path)
      :flatten (.flatten path)
      :reverse (.reverse path))
    (get-d path)))

(mx/defn boolean-operation :- string?
  [path-a :- string?, path-b  :- string?, operation :- PathBooleanOperation]
  (let [path-a (Path. path-a)
        path-b (Path. path-b)]
    (get-d (case operation
             :unite (.unite path-a path-b)
             :intersect (.intersect path-a path-b)
             :subtract (.subtract path-a path-b)
             :exclude (.exclude path-a path-b)
             :divide (.divide path-a path-b)))))
