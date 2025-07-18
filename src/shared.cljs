(ns shared)

(def save-keys [:id :title :path])

(defn document->save-format
  [document]
  (-> (apply dissoc document save-keys)
      (pr-str)))
