(ns shared)

(defn document->save-format
  [document]
  (-> document
      (dissoc :path :id :title)
      (pr-str)))
