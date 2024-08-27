(ns renderer.utils.uuid
  (:require
   [malli.experimental :as mx]))

(mx/defn generate :- keyword?
  []
  (-> (random-uuid)
      (str)
      (keyword)))

(mx/defn generate-unique :- keyword?
  [existing? :- fn?]
  (loop [id (generate)]
    (if-not (existing? id)
      id
      (recur (generate)))))
