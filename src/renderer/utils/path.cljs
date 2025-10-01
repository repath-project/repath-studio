(ns renderer.utils.path
  (:require
   ["paper" :refer [Path]]
   [malli.core :as m]
   [renderer.db :refer [BooleanOperation PathManipulation]]))

(m/=> get-d [:-> any? string?])
(defn get-d
  [paper-path]
  (-> paper-path
      (.exportSVG)
      (.getAttribute "d")))

(m/=> manipulate [:-> string? PathManipulation string?])
(defn manipulate
  [path manipulation]
  (let [path (Path. path)]
    (case manipulation
      :simplify (.simplify path)
      :smooth (.smooth path)
      :flatten (.flatten path)
      :reverse (.reverse path))
    (get-d path)))

(m/=> boolean-operation [:-> string? string? BooleanOperation string?])
(defn boolean-operation
  [path-a path-b operation]
  (let [path-a (Path. path-a)
        path-b (Path. path-b)]
    (get-d (case operation
             :unite (.unite path-a path-b)
             :intersect (.intersect path-a path-b)
             :subtract (.subtract path-a path-b)
             :exclude (.exclude path-a path-b)
             :divide (.divide path-a path-b)))))
