(ns renderer.utils.uuid)

(defn generate []
  (-> (random-uuid)
      str
      keyword)) ; REVIEW
