(ns renderer.utils.uuid)

(defn generate
  []
  (-> (random-uuid)
      str
      keyword))

(defn generate-unique
  [existing?]
  (loop [id (generate)]
    (if-not (existing? id)
      id
      (recur (generate)))))
